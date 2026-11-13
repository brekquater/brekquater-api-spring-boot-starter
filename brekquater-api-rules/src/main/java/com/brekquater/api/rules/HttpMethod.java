// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Locale;

/**
 * HTTP methods an OpenAPI path item can declare. Operations are indexed in this declaration order, so adding a
 * method here is the only change a new method needs.
 */
public enum HttpMethod {
    GET,
    PUT,
    POST,
    DELETE,
    OPTIONS,
    HEAD,
    PATCH,
    TRACE;

    /** Returns the key this method has inside an OpenAPI path item, for example {@code get}. */
    public String pathItemKey() {
        return name().toLowerCase(Locale.ROOT);
    }
}
