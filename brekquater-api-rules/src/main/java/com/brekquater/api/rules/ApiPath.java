// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * One key under {@code paths}, for example {@code /orders/{id}}.
 *
 * @param path the path template as written in the document
 * @param pointer the location of the path item in the document
 */
public record ApiPath(String path, JsonPointer pointer) {

    private static final String SEPARATOR = "/";
    private static final String PARAMETER_START = "{";

    public ApiPath {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(pointer, "pointer");
    }

    /** Returns the non-empty segments that contain no path parameter, in order. */
    public List<String> literalSegments() {
        return Arrays.stream(path.split(SEPARATOR))
                .filter(Predicate.not(String::isEmpty))
                .filter(segment -> !segment.contains(PARAMETER_START))
                .toList();
    }
}
