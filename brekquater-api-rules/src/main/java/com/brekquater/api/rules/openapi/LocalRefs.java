// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** The named schemas of a document, and the only kind of {@code $ref} that is ever followed: one that names them. */
final class LocalRefs {

    private static final String PREFIX = "#/components/schemas/";

    private final Map<String, Schema<?>> named = new TreeMap<>();

    LocalRefs(OpenAPI openApi) {
        io.swagger.v3.oas.models.Components components = openApi.getComponents();
        Map<?, ?> schemas = components == null || components.getSchemas() == null ? Map.of() : components.getSchemas();
        schemas.forEach((name, schema) -> {
            if (schema instanceof Schema<?> present) {
                named.put(String.valueOf(name), present);
            }
        });
    }

    Map<String, Schema<?>> named() {
        return named;
    }

    Optional<Schema<?>> resolve(String reference) {
        return Optional.ofNullable(reference)
                .filter(ref -> ref.startsWith(PREFIX))
                .map(ref -> named.get(ref.substring(PREFIX.length())));
    }
}
