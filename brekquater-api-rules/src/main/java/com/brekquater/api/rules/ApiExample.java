// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;
import java.util.Objects;

/**
 * One example or default value, already checked against the schema it illustrates.
 *
 * @param pointer the location of the value in the document
 * @param origin where the value is written
 * @param problems why the value does not match its schema, in a fixed order; empty when it matches
 */
public record ApiExample(JsonPointer pointer, Origin origin, List<String> problems) {

    /** Where an example is written. */
    public enum Origin {
        /** An {@code example}, {@code examples} or {@code default} of a schema. */
        SCHEMA,
        /** An example of a parameter, or of a JSON request or response body. */
        CONTENT
    }

    public ApiExample {
        Objects.requireNonNull(pointer, "pointer");
        Objects.requireNonNull(origin, "origin");
        problems = List.copyOf(problems);
    }
}
