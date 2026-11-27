// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiSchemaNode;
import com.brekquater.api.rules.ApiSchemaShape;
import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The one walk over every schema of a document. Named schemas come first, in name order, then the schemas of each
 * path and operation, in operation order. A {@code $ref} is recorded but not followed, because the schema it names
 * is walked on its own.
 */
final class SchemaIndex {

    private static final JsonPointer COMPONENT_SCHEMAS = JsonPointer.root().append("components").append("schemas");
    private static final JsonPointer PATHS = JsonPointer.root().append("paths");
    private static final String PARAMETERS = "parameters";
    private static final String SCHEMA = "schema";
    private static final String CONTENT = "content";
    // ponytail: a depth cap instead of cycle detection. A parsed document cannot loop without a $ref, which is not
    // followed, so only a document built in code could; switch to an identity set if one ever needs more depth.
    static final int MAX_DEPTH = 64;

    private final List<Visit> visits = new ArrayList<>();

    private SchemaIndex() {}

    static List<ApiSchemaNode> nodes(List<Visit> visits, LocalRefs refs) {
        SchemaShapes shapes = new SchemaShapes(refs);
        return visits.stream().map(visit -> node(visit, shapes.of(visit.schema()))).toList();
    }

    static List<Visit> of(OpenAPI openApi) {
        SchemaIndex index = new SchemaIndex();
        io.swagger.v3.oas.models.Components components = openApi.getComponents();
        Map<String, ?> named = Optional.<Map<String, ?>>ofNullable(components == null ? null : components.getSchemas())
                .orElse(Map.of());
        new TreeMap<>(named).forEach((name, schema) -> index.walk(schema, COMPONENT_SCHEMAS.append(name), 0));
        OperationIndex.sortedPaths(openApi).forEach(index::walkPathItem);
        OperationIndex.sources(openApi).forEach(index::walkOperation);
        return List.copyOf(index.visits);
    }

    private void walkPathItem(String path, PathItem pathItem) {
        walkParameters(pathItem.getParameters(), PATHS.append(path));
    }

    private void walkOperation(OperationSource source) {
        JsonPointer pointer = PATHS.append(source.path()).append(source.method().pathItemKey());
        walkParameters(source.operation().getParameters(), pointer);
        Optional.ofNullable(source.operation().getRequestBody())
                .ifPresent(body -> walkContent(body.getContent(), pointer.append("requestBody").append(CONTENT)));
        walkResponses(source.operation().getResponses(), pointer.append("responses"));
    }

    private void walkParameters(List<Parameter> parameters, JsonPointer owner) {
        List<Parameter> declared = Optional.ofNullable(parameters).orElse(List.of());
        for (int index = 0; index < declared.size(); index++) {
            Optional<Parameter> parameter = Optional.ofNullable(declared.get(index));
            JsonPointer pointer = owner.append(PARAMETERS).append(String.valueOf(index)).append(SCHEMA);
            parameter.ifPresent(present -> walk(present.getSchema(), pointer, 0));
        }
    }

    private void walkResponses(ApiResponses responses, JsonPointer pointer) {
        Optional.ofNullable(responses).ifPresent(declared -> new TreeMap<>(declared).forEach((code, response) -> {
            if (response != null) {
                walkContent(response.getContent(), pointer.append(code).append(CONTENT));
            }
        }));
    }

    private void walkContent(Content content, JsonPointer pointer) {
        Optional.ofNullable(content).ifPresent(mediaTypes -> new TreeMap<>(mediaTypes).forEach((name, mediaType) -> {
            if (mediaType != null) {
                walk(mediaType.getSchema(), pointer.append(name).append(SCHEMA), 0);
            }
        }));
    }

    // Takes any value, because swagger-models types nested schemas loosely: additionalProperties may be a Boolean.
    private void walk(Object candidate, JsonPointer pointer, int depth) {
        if (!(candidate instanceof Schema<?> schema) || depth > MAX_DEPTH) {
            return;
        }
        visits.add(new Visit(pointer, schema));
        if (schema.get$ref() == null) {
            walkChildren(schema, pointer, depth + 1);
        }
    }

    private void walkChildren(Schema<?> schema, JsonPointer pointer, int depth) {
        Map<String, ?> properties = Optional.<Map<String, ?>>ofNullable(schema.getProperties()).orElse(Map.of());
        new TreeMap<>(properties).forEach((name, child) -> walk(child, pointer.append("properties").append(name), depth));
        walk(schema.getItems(), pointer.append("items"), depth);
        walkEach(schema.getAllOf(), pointer.append("allOf"), depth);
        walkEach(schema.getAnyOf(), pointer.append("anyOf"), depth);
        walkEach(schema.getOneOf(), pointer.append("oneOf"), depth);
        walk(schema.getNot(), pointer.append("not"), depth);
        walk(schema.getAdditionalProperties(), pointer.append("additionalProperties"), depth);
    }

    private static ApiSchemaNode node(Visit visit, ApiSchemaShape shape) {
        Schema<?> schema = visit.schema();
        return new ApiSchemaNode(
                visit.pointer(),
                Optional.ofNullable(schema.getPattern()),
                Optional.ofNullable(schema.get$ref()),
                Boolean.TRUE.equals(schema.getWriteOnly()),
                shape);
    }

    /** One schema the walk reached, and where. */
    record Visit(JsonPointer pointer, Schema<?> schema) {}

    private void walkEach(List<?> schemas, JsonPointer pointer, int depth) {
        List<?> branches = Optional.<List<?>>ofNullable(schemas).orElse(List.of());
        for (int index = 0; index < branches.size(); index++) {
            walk(branches.get(index), pointer.append(String.valueOf(index)), depth);
        }
    }
}
