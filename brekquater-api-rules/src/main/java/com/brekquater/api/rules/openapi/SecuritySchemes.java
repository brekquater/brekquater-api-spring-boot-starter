// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiSecurityScheme;
import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Reads the document's {@code components.securitySchemes}, ordered by name. */
final class SecuritySchemes {

    static final JsonPointer POINTER = JsonPointer.root().append("components").append("securitySchemes");
    private static final String UNKNOWN_TYPE = "";

    private SecuritySchemes() {}

    /** Returns the declared schemes by name, in name order, leaving out entries without a scheme. */
    static TreeMap<String, SecurityScheme> declared(OpenAPI openApi) {
        Map<String, SecurityScheme> schemes = Optional.ofNullable(openApi.getComponents())
                .map(io.swagger.v3.oas.models.Components::getSecuritySchemes)
                .orElse(Map.of());
        TreeMap<String, SecurityScheme> declared = new TreeMap<>();
        schemes.forEach((name, scheme) -> {
            if (Objects.nonNull(scheme)) {
                declared.put(name, scheme);
            }
        });
        return declared;
    }

    static List<ApiSecurityScheme> of(OpenAPI openApi) {
        return declared(openApi).entrySet().stream()
                .map(entry -> view(entry.getKey(), entry.getValue()))
                .toList();
    }

    // swagger-models writes each enum value the way the document spells it, for example "apiKey" or "query".
    private static ApiSecurityScheme view(String name, SecurityScheme scheme) {
        return new ApiSecurityScheme(
                name,
                Optional.ofNullable(scheme.getType()).map(String::valueOf).orElse(UNKNOWN_TYPE),
                Optional.ofNullable(scheme.getScheme()),
                Optional.ofNullable(scheme.getIn()).map(String::valueOf),
                POINTER.append(name));
    }
}
