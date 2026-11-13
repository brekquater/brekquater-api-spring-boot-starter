// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Arrays;
import java.util.List;

/**
 * JSON Pointer as defined by RFC 6901, used as the evidence location of OpenAPI findings.
 *
 * <p>Build one from {@link #root()} with {@link #append(String)}; escaping is handled here and nowhere else.
 *
 * @param value the escaped pointer text, for example {@code /paths/~1orders~1{id}/get}
 */
public record JsonPointer(String value) {

    private static final String SEPARATOR = "/";
    private static final JsonPointer ROOT = new JsonPointer("");

    public JsonPointer {
        if (!value.isEmpty() && !value.startsWith(SEPARATOR)) {
            throw new IllegalArgumentException("A JSON Pointer is empty or starts with '/': " + value);
        }
    }

    /** Returns the pointer to the whole document. */
    public static JsonPointer root() {
        return ROOT;
    }

    /** Returns a pointer one level deeper, to the member named by the unescaped {@code token}. */
    public JsonPointer append(String token) {
        return new JsonPointer(value + SEPARATOR + escape(token));
    }

    /** Returns the unescaped reference tokens, from the document root downwards. */
    public List<String> tokens() {
        if (value.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(value.substring(1).split(SEPARATOR, -1))
                .map(JsonPointer::unescape)
                .toList();
    }

    @Override
    public String toString() {
        return value;
    }

    // RFC 6901 section 3: '~' must be escaped before '/', or the '~' of a fresh "~1" would be escaped again.
    private static String escape(String token) {
        return token.replace("~", "~0").replace("/", "~1");
    }

    // RFC 6901 section 4: "~1" must be decoded before "~0", so that "~01" becomes "~1" and not "/".
    private static String unescape(String token) {
        return token.replace("~1", "/").replace("~0", "~");
    }
}
