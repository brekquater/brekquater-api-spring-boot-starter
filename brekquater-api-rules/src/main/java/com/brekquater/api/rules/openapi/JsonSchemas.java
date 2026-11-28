// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import io.swagger.v3.oas.models.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Writes a swagger-models schema as JSON Schema 2020-12, copying only the keywords that decide whether a value is
 * valid. The same code covers OpenAPI 3.0, whose {@code nullable} and boolean exclusive bounds are translated.
 *
 * <p>A {@code $ref} is copied only when it names a schema under {@code components}. Any other is dropped, so the
 * validator is never given an address it could try to open.
 */
final class JsonSchemas {

    private static final String NULL = "null";

    private final LocalRefs refs;
    private final ObjectNode named = JsonValues.MAPPER.createObjectNode();

    JsonSchemas(LocalRefs refs) {
        this.refs = refs;
        refs.named().forEach((name, schema) -> named.set(name, convert(schema, 0)));
    }

    /** Returns the schema as a standalone JSON Schema document that carries the named schemas it may refer to. */
    ObjectNode standalone(Schema<?> schema) {
        ObjectNode root = JsonValues.MAPPER.createObjectNode();
        root.putArray("allOf").add(convert(schema, 0));
        root.putObject("components").set("schemas", named);
        return root;
    }

    // ponytail: the same depth cap as SchemaIndex, for documents built in code that loop without a $ref.
    private ObjectNode convert(Schema<?> schema, int depth) {
        ObjectNode json = JsonValues.MAPPER.createObjectNode();
        if (depth > SchemaIndex.MAX_DEPTH) {
            return json;
        }
        if (schema.get$ref() != null) {
            refs.resolve(schema.get$ref()).ifPresent(target -> json.put("$ref", schema.get$ref()));
            return json;
        }
        values(schema, json);
        bounds(schema, json);
        children(schema, json, depth + 1);
        return json;
    }

    private static void values(Schema<?> schema, ObjectNode json) {
        boolean nullable = Boolean.TRUE.equals(schema.getNullable());
        types(schema, json, nullable);
        if (schema.getEnum() != null) {
            ArrayNode allowed = json.putArray("enum");
            schema.getEnum().forEach(value -> allowed.add(JsonValues.tree(value)));
            if (nullable) {
                allowed.addNull();
            }
        }
        put(json, "const", schema.getConst());
        put(json, "pattern", schema.getPattern());
    }

    private static void types(Schema<?> schema, ObjectNode json, boolean nullable) {
        Set<String> types = new TreeSet<>(SchemaFacts.types(schema));
        if (types.isEmpty()) {
            return;
        }
        if (nullable) {
            types.add(NULL);
        }
        ArrayNode declared = json.putArray("type");
        types.forEach(declared::add);
    }

    private static void bounds(Schema<?> schema, ObjectNode json) {
        bound(json, "minimum", schema.getMinimum(), schema.getExclusiveMinimum());
        bound(json, "maximum", schema.getMaximum(), schema.getExclusiveMaximum());
        put(json, "exclusiveMinimum", schema.getExclusiveMinimumValue());
        put(json, "exclusiveMaximum", schema.getExclusiveMaximumValue());
        put(json, "multipleOf", schema.getMultipleOf());
        put(json, "minLength", schema.getMinLength());
        put(json, "maxLength", schema.getMaxLength());
        put(json, "minItems", schema.getMinItems());
        put(json, "maxItems", schema.getMaxItems());
        put(json, "uniqueItems", schema.getUniqueItems());
        put(json, "minProperties", schema.getMinProperties());
        put(json, "maxProperties", schema.getMaxProperties());
    }

    // OpenAPI 3.0 marks a limit as excluded with a boolean; JSON Schema 2020-12 uses a different keyword.
    private static void bound(ObjectNode json, String keyword, BigDecimal limit, Boolean excluded) {
        String exclusive = "exclusive" + Character.toUpperCase(keyword.charAt(0)) + keyword.substring(1);
        put(json, Boolean.TRUE.equals(excluded) ? exclusive : keyword, limit);
    }

    private void children(Schema<?> schema, ObjectNode json, int depth) {
        properties(schema, json, depth);
        required(schema, json);
        child(json, "items", schema.getItems(), depth);
        child(json, "not", schema.getNot(), depth);
        if (schema.getAdditionalProperties() instanceof Boolean allowed) {
            json.put("additionalProperties", allowed.booleanValue());
        }
        child(json, "additionalProperties", schema.getAdditionalProperties(), depth);
        branches(json, "allOf", schema.getAllOf(), depth);
        // ponytail: "oneOf" is checked as "anyOf". Subtypes that are not closed overlap, so a correct example of
        // one would match several branches; the cost is that a value matching two exclusive branches passes.
        branches(json, "anyOf", schema.getAnyOf() != null ? schema.getAnyOf() : schema.getOneOf(), depth);
    }

    private void properties(Schema<?> schema, ObjectNode json, int depth) {
        Map<?, ?> properties = schema.getProperties() == null ? Map.of() : schema.getProperties();
        if (!properties.isEmpty()) {
            ObjectNode declared = json.putObject("properties");
            properties.forEach((name, value) -> child(declared, String.valueOf(name), value, depth));
        }
    }

    // A readOnly property is not sent in requests and a writeOnly one is not returned, so neither can be demanded
    // of an example without knowing which direction it illustrates.
    private static void required(Schema<?> schema, ObjectNode json) {
        List<String> names = schema.getRequired() == null ? List.of() : schema.getRequired();
        Map<?, ?> properties = schema.getProperties() == null ? Map.of() : schema.getProperties();
        List<String> demanded = names.stream().filter(name -> !oneWay(properties.get(name))).toList();
        if (!demanded.isEmpty()) {
            ArrayNode required = json.putArray("required");
            demanded.forEach(required::add);
        }
    }

    private static boolean oneWay(Object property) {
        return property instanceof Schema<?> schema
                && (Boolean.TRUE.equals(schema.getReadOnly()) || Boolean.TRUE.equals(schema.getWriteOnly()));
    }

    private void child(ObjectNode json, String keyword, Object candidate, int depth) {
        if (candidate instanceof Schema<?> schema) {
            json.set(keyword, convert(schema, depth));
        }
    }

    private void branches(ObjectNode json, String keyword, List<?> schemas, int depth) {
        if (schemas == null || schemas.isEmpty()) {
            return;
        }
        ArrayNode branches = json.putArray(keyword);
        schemas.forEach(candidate -> {
            if (candidate instanceof Schema<?> schema) {
                branches.add(convert(schema, depth));
            }
        });
    }

    private static void put(ObjectNode json, String keyword, Object value) {
        if (value != null) {
            json.set(keyword, JsonValues.tree(value));
        }
    }
}
