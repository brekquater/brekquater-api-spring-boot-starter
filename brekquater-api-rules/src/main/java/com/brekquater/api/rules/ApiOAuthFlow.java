// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One flow of an OAuth 2.0 security scheme.
 *
 * @param scheme the name of the security scheme the flow belongs to
 * @param type the flow as OpenAPI names it: {@code implicit}, {@code password}, {@code clientCredentials} or
 *     {@code authorizationCode}
 * @param pointer the location of the flow in the document
 * @param scopes the scopes the flow declares
 * @param urls the authorization, token and refresh URLs the flow gives, in that order
 */
public record ApiOAuthFlow(String scheme, String type, JsonPointer pointer, Set<String> scopes, List<String> urls) {

    public ApiOAuthFlow {
        Objects.requireNonNull(scheme, "scheme");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(pointer, "pointer");
        scopes = Set.copyOf(scopes);
        urls = List.copyOf(urls);
    }
}
