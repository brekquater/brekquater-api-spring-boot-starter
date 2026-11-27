// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;

/**
 * A fixed value written in the document, such as one entry of an {@code enum}.
 *
 * @param json the value as JSON text, so the string {@code "1"} and the number {@code 1} differ
 * @param type its JSON Schema type: string, integer, number, boolean, object, array or null
 */
public record ApiLiteral(String json, String type) {

    public ApiLiteral {
        Objects.requireNonNull(json, "json");
        Objects.requireNonNull(type, "type");
    }
}
