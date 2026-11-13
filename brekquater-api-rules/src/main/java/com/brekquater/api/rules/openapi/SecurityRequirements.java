// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiSecurityRequirement;
import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

/** Turns a swagger-models {@code security} list into {@link ApiSecurityRequirement} views, keeping list order. */
final class SecurityRequirements {

    private SecurityRequirements() {}

    static List<ApiSecurityRequirement> of(List<SecurityRequirement> requirements, JsonPointer security) {
        return IntStream.range(0, requirements.size())
                .mapToObj(index -> new ApiSecurityRequirement(
                        security.append(String.valueOf(index)), schemeNames(requirements.get(index))))
                .toList();
    }

    private static Set<String> schemeNames(SecurityRequirement requirement) {
        return Optional.<Map<String, List<String>>>ofNullable(requirement)
                .map(Map::keySet)
                .orElse(Set.of());
    }
}
