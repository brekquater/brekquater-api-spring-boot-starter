// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/** Turns a swagger-models schema into the version-neutral {@link ApiSchema}. */
final class SchemaFacts {

    private SchemaFacts() {}

    static ApiSchema of(Schema<?> schema) {
        return new ApiSchema(
                schema.get$ref() != null,
                types(schema),
                schema.getProperties() != null && !schema.getProperties().isEmpty(),
                hasComposition(schema),
                hasTypedAdditionalProperties(schema));
    }

    // OpenAPI 3.0 documents fill "type"; 3.1 documents fill "types". Either may be the only one present.
    private static Set<String> types(Schema<?> schema) {
        Set<String> types = new HashSet<>(Optional.<Set<String>>ofNullable(schema.getTypes()).orElse(Set.of()));
        Optional.ofNullable(schema.getType()).ifPresent(types::add);
        return types;
    }

    private static boolean hasComposition(Schema<?> schema) {
        return isNotEmpty(schema.getAllOf())
                || isNotEmpty(schema.getAnyOf())
                || isNotEmpty(schema.getOneOf())
                || schema.getNot() != null;
    }

    // "additionalProperties: true" or "{}" allows any extra value; only a schema with a shape constrains them.
    private static boolean hasTypedAdditionalProperties(Schema<?> schema) {
        return schema.getAdditionalProperties() instanceof Schema<?> additional && hasShape(additional);
    }

    private static boolean hasShape(Schema<?> schema) {
        return schema.get$ref() != null || !types(schema).isEmpty() || hasComposition(schema);
    }

    private static boolean isNotEmpty(Collection<?> values) {
        return values != null && !values.isEmpty();
    }
}
