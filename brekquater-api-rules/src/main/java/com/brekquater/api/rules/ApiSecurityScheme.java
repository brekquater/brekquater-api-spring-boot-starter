// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;
import java.util.Optional;

/**
 * One security scheme under {@code components.securitySchemes}.
 *
 * @param name the name operations use to require the scheme
 * @param type the scheme type as OpenAPI names it: {@code http}, {@code apiKey}, {@code oauth2},
 *     {@code openIdConnect} or {@code mutualTLS}
 * @param httpScheme for an {@code http} scheme, the HTTP authentication scheme, such as {@code bearer}
 * @param apiKeyLocation for an {@code apiKey} scheme, where the key is sent: {@code query}, {@code header} or
 *     {@code cookie}
 * @param pointer the location of the scheme in the document
 */
public record ApiSecurityScheme(
        String name, String type, Optional<String> httpScheme, Optional<String> apiKeyLocation, JsonPointer pointer) {

    private static final String HTTP = "http";
    private static final String API_KEY = "apiKey";

    public ApiSecurityScheme {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(httpScheme, "httpScheme");
        Objects.requireNonNull(apiKeyLocation, "apiKeyLocation");
        Objects.requireNonNull(pointer, "pointer");
    }

    public boolean isHttp() {
        return type.equals(HTTP);
    }

    public boolean isApiKey() {
        return type.equals(API_KEY);
    }

    /** Returns whether this is an {@code http} scheme using the given HTTP scheme, ignoring case as HTTP does. */
    public boolean usesHttpScheme(String scheme) {
        return isHttp() && httpScheme.filter(scheme::equalsIgnoreCase).isPresent();
    }
}
