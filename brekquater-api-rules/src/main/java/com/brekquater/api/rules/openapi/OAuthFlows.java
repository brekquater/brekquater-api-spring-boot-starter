// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiOAuthFlow;
import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Lists the flows of the document's OAuth 2.0 security schemes, ordered by scheme name and then by flow. */
final class OAuthFlows {

    private static final JsonPointer SCHEMES = JsonPointer.root().append("components").append("securitySchemes");
    private static final String FLOWS = "flows";

    private OAuthFlows() {}

    static List<ApiOAuthFlow> of(OpenAPI openApi) {
        Map<String, SecurityScheme> schemes = Optional.ofNullable(openApi.getComponents())
                .map(io.swagger.v3.oas.models.Components::getSecuritySchemes)
                .orElse(Map.of());
        return new TreeMap<>(schemes)
                .entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getFlows() != null)
                .flatMap(entry -> flowsOf(entry.getValue().getFlows(), SCHEMES.append(entry.getKey()).append(FLOWS)))
                .toList();
    }

    private static Stream<ApiOAuthFlow> flowsOf(io.swagger.v3.oas.models.security.OAuthFlows flows, JsonPointer pointer) {
        // The four flows OpenAPI defines, under the names the document uses for them.
        Map<String, OAuthFlow> declared = new LinkedHashMap<>();
        declared.put("authorizationCode", flows.getAuthorizationCode());
        declared.put("clientCredentials", flows.getClientCredentials());
        declared.put("implicit", flows.getImplicit());
        declared.put("password", flows.getPassword());
        return declared.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> new ApiOAuthFlow(entry.getKey(), pointer.append(entry.getKey())));
    }
}
