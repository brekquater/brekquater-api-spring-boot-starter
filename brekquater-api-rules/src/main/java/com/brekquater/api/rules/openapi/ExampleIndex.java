// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiExample;
import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** Every example and default of a document, validated: those of schemas, then those of parameters and bodies. */
final class ExampleIndex {

    private static final JsonPointer PATHS = JsonPointer.root().append("paths");
    private static final Set<String> STRUCTURED = Set.of("array", "object");
    private static final String JSON = "json";
    private static final String CONTENT = "content";
    private static final String EXAMPLE = "example";
    private static final String EXAMPLES = "examples";

    private final ExampleValidator validator;
    private final List<ApiExample> examples = new ArrayList<>();

    private ExampleIndex(ExampleValidator validator) {
        this.validator = validator;
    }

    static List<ApiExample> of(OpenAPI openApi, List<SchemaIndex.Visit> schemas) {
        ExampleIndex index = new ExampleIndex(new ExampleValidator(new LocalRefs(openApi)));
        schemas.forEach(index::schemaValues);
        OperationIndex.sortedPaths(openApi)
                .forEach((path, item) -> index.parameters(item.getParameters(), PATHS.append(path)));
        OperationIndex.sources(openApi).forEach(index::operation);
        return List.copyOf(index.examples);
    }

    private void schemaValues(SchemaIndex.Visit visit) {
        Schema<?> schema = visit.schema();
        schemaValue(schema.getDefault(), schema, visit.pointer().append("default"));
        schemaValue(schema.getExample(), schema, visit.pointer().append(EXAMPLE));
        List<?> several = schema.getExamples() == null ? List.of() : schema.getExamples();
        for (int index = 0; index < several.size(); index++) {
            schemaValue(several.get(index), schema, visit.pointer().append(EXAMPLES).append(String.valueOf(index)));
        }
    }

    private void schemaValue(Object value, Schema<?> schema, JsonPointer pointer) {
        if (value != null) {
            examples.add(new ApiExample(pointer, ApiExample.Origin.SCHEMA, validator.problems(value, schema)));
        }
    }

    private void operation(OperationSource source) {
        JsonPointer pointer = PATHS.append(source.path()).append(source.method().pathItemKey());
        parameters(source.operation().getParameters(), pointer);
        Optional.ofNullable(source.operation().getRequestBody())
                .ifPresent(body -> content(body.getContent(), pointer.append("requestBody").append(CONTENT)));
        Optional.ofNullable(source.operation().getResponses()).ifPresent(responses -> new TreeMap<>(responses)
                .forEach((code, response) -> {
                    if (response != null) {
                        content(response.getContent(), pointer.append("responses").append(code).append(CONTENT));
                    }
                }));
    }

    private void parameters(List<Parameter> parameters, JsonPointer owner) {
        List<Parameter> declared = parameters == null ? List.of() : parameters;
        for (int index = 0; index < declared.size(); index++) {
            Parameter parameter = declared.get(index);
            if (parameter != null && parameter.getSchema() != null) {
                parameter(parameter, owner.append("parameters").append(String.valueOf(index)));
            }
        }
    }

    private void parameter(Parameter parameter, JsonPointer pointer) {
        Schema<?> schema = parameter.getSchema();
        parameterValue(parameter.getExample(), schema, pointer.append(EXAMPLE));
        named(parameter.getExamples()).forEach((name, example) ->
                parameterValue(example.getValue(), schema, pointer.append(EXAMPLES).append(name)));
    }

    // A list or an object is written into a parameter in its own style, such as "a,b", which is not JSON.
    private void parameterValue(Object value, Schema<?> schema, JsonPointer pointer) {
        boolean styled = value instanceof String && SchemaFacts.types(schema).stream().anyMatch(STRUCTURED::contains);
        if (value != null && !styled) {
            examples.add(new ApiExample(pointer, ApiExample.Origin.CONTENT, validator.problemsOfText(value, schema)));
        }
    }

    // Only JSON bodies: an XML or CSV example is text that a JSON Schema cannot check.
    private void content(Content content, JsonPointer pointer) {
        Optional.ofNullable(content).ifPresent(mediaTypes -> new TreeMap<>(mediaTypes).forEach((name, mediaType) -> {
            if (mediaType != null && mediaType.getSchema() != null && name.toLowerCase(Locale.ROOT).contains(JSON)) {
                body(mediaType, pointer.append(name));
            }
        }));
    }

    private void body(MediaType mediaType, JsonPointer pointer) {
        Schema<?> schema = mediaType.getSchema();
        bodyValue(mediaType.getExample(), schema, pointer.append(EXAMPLE));
        named(mediaType.getExamples()).forEach((name, example) ->
                bodyValue(example.getValue(), schema, pointer.append(EXAMPLES).append(name)));
    }

    private void bodyValue(Object value, Schema<?> schema, JsonPointer pointer) {
        if (value != null) {
            examples.add(new ApiExample(pointer, ApiExample.Origin.CONTENT, validator.problems(value, schema)));
        }
    }

    private static Map<String, Example> named(Map<String, Example> examples) {
        Map<String, Example> sorted = new TreeMap<>();
        if (examples != null) {
            examples.forEach((name, example) -> {
                if (example != null) {
                    sorted.put(name, example);
                }
            });
        }
        return sorted;
    }
}
