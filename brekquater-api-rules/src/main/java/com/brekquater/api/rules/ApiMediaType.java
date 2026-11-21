// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * One media type of a request or response body, for example {@code application/json}.
 *
 * @param name the media type as written in the document
 * @param pointer the location of the media type in the document
 * @param schema the schema of the body, or empty when the document gives none
 */
public record ApiMediaType(String name, JsonPointer pointer, Optional<ApiSchema> schema) {

    private static final String PROBLEM_DETAILS_PREFIX = "application/problem+";

    public ApiMediaType {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(pointer, "pointer");
        Objects.requireNonNull(schema, "schema");
    }

    /** Returns whether this is an RFC 9457 problem details type, in its JSON or XML form. */
    public boolean isProblemDetails() {
        return name.toLowerCase(Locale.ROOT).startsWith(PROBLEM_DETAILS_PREFIX);
    }
}
