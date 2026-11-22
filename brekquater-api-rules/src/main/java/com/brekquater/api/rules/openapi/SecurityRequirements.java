// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiSecurityRequirement;
import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.IntStream;

/** Turns a swagger-models {@code security} list into {@link ApiSecurityRequirement} views, keeping list order. */
final class SecurityRequirements {

    private SecurityRequirements() {}

    static List<ApiSecurityRequirement> of(List<SecurityRequirement> requirements, JsonPointer security) {
        return IntStream.range(0, requirements.size())
                .mapToObj(index -> new ApiSecurityRequirement(
                        security.append(String.valueOf(index)), scopes(requirements.get(index))))
                .toList();
    }

    // A scheme listed without scopes has a null list in a document built in code.
    private static Map<String, List<String>> scopes(SecurityRequirement requirement) {
        Map<String, List<String>> scopes = new TreeMap<>();
        Optional.<Map<String, List<String>>>ofNullable(requirement).orElse(Map.of())
                .forEach((scheme, required) -> scopes.put(scheme, Optional.ofNullable(required).orElse(List.of())));
        return scopes;
    }
}
