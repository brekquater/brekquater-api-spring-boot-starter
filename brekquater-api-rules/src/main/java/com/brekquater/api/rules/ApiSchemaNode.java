// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * One schema anywhere in the document: a named schema, one of its properties, the items of an array, a branch of
 * {@code allOf}, or the schema of a parameter or a body.
 *
 * @param pointer the location of the schema in the document
 * @param pattern the regular expression a string must match, when the schema declares one
 */
public record ApiSchemaNode(JsonPointer pointer, Optional<String> pattern) {

    private static final String PROPERTIES = "properties";

    public ApiSchemaNode {
        Objects.requireNonNull(pointer, "pointer");
        Objects.requireNonNull(pattern, "pattern");
    }

    /** Returns the property name when this schema is a property of an object, which is when it sits under {@code properties}. */
    public Optional<String> propertyName() {
        List<String> tokens = pointer.tokens();
        int last = tokens.size() - 1;
        if (last < 1 || !tokens.get(last - 1).equals(PROPERTIES)) {
            return Optional.empty();
        }
        return Optional.of(tokens.get(last));
    }
}
