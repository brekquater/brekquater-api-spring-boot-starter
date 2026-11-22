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
 * @param reference the {@code $ref} of the schema, when it refers to another one
 * @param writeOnly whether the schema is marked {@code writeOnly}, so it is sent in requests but never returned
 */
public record ApiSchemaNode(JsonPointer pointer, Optional<String> pattern, Optional<String> reference, boolean writeOnly) {

    private static final String PROPERTIES = "properties";
    private static final String NAMED_SCHEMA = "#/components/schemas/";

    public ApiSchemaNode {
        Objects.requireNonNull(pointer, "pointer");
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(reference, "reference");
    }

    /** Returns the name of the schema under {@code components.schemas} that this schema refers to, if any. */
    public Optional<String> referencedSchemaName() {
        return reference.filter(ref -> ref.startsWith(NAMED_SCHEMA)).map(ref -> ref.substring(NAMED_SCHEMA.length()));
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
