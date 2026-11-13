// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Set;

/**
 * Structural facts about a schema. OpenAPI 3.0 writes a single {@code type} and 3.1 a list of {@code types}; this
 * view always gives the list, so rules never need to know which version they read.
 *
 * @param reference whether the schema refers to a named schema with {@code $ref}
 * @param types the declared types, such as {@code object} or {@code array}; empty when none is declared
 * @param hasProperties whether the schema lists any properties
 * @param hasComposition whether the schema is built with {@code allOf}, {@code anyOf}, {@code oneOf} or {@code not}
 * @param hasTypedAdditionalProperties whether {@code additionalProperties} is a schema with a structure of its own
 */
public record ApiSchema(
        boolean reference,
        Set<String> types,
        boolean hasProperties,
        boolean hasComposition,
        boolean hasTypedAdditionalProperties) {

    private static final String OBJECT = "object";
    private static final String ARRAY = "array";

    public ApiSchema {
        types = Set.copyOf(types);
    }

    public boolean isArray() {
        return types.contains(ARRAY);
    }

    /**
     * Returns whether the schema accepts any object at all: an object, or an untyped schema, that names no
     * properties, no composition and no type for extra properties.
     */
    public boolean isFreeFormObject() {
        boolean untypedOrObject = types.isEmpty() || types.equals(Set.of(OBJECT));
        return !reference && untypedOrObject && !hasProperties && !hasComposition && !hasTypedAdditionalProperties;
    }
}
