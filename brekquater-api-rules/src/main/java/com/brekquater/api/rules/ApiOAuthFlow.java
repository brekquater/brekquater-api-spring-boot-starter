// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;

/**
 * One flow of an OAuth 2.0 security scheme.
 *
 * @param type the flow as OpenAPI names it: {@code implicit}, {@code password}, {@code clientCredentials} or
 *     {@code authorizationCode}
 * @param pointer the location of the flow in the document
 */
public record ApiOAuthFlow(String type, JsonPointer pointer) {

    public ApiOAuthFlow {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(pointer, "pointer");
    }
}
