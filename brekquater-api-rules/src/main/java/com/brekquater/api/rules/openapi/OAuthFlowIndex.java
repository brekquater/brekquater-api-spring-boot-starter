// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiOAuthFlow;
import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/** Lists the flows of the document's OAuth 2.0 security schemes, ordered by scheme name and then by flow. */
final class OAuthFlowIndex {

    private static final String FLOWS = "flows";

    private OAuthFlowIndex() {}

    static List<ApiOAuthFlow> of(OpenAPI openApi) {
        return SecuritySchemes.declared(openApi).entrySet().stream()
                .filter(entry -> entry.getValue().getFlows() != null)
                .flatMap(entry -> flowsOf(entry.getKey(), entry.getValue().getFlows()))
                .toList();
    }

    private static Stream<ApiOAuthFlow> flowsOf(String scheme, OAuthFlows flows) {
        JsonPointer pointer = SecuritySchemes.POINTER.append(scheme).append(FLOWS);
        // The four flows OpenAPI defines, under the names the document uses for them.
        Map<String, OAuthFlow> declared = new LinkedHashMap<>();
        declared.put("authorizationCode", flows.getAuthorizationCode());
        declared.put("clientCredentials", flows.getClientCredentials());
        declared.put("implicit", flows.getImplicit());
        declared.put("password", flows.getPassword());
        return declared.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> flow(scheme, entry.getKey(), entry.getValue(), pointer.append(entry.getKey())));
    }

    private static ApiOAuthFlow flow(String scheme, String type, OAuthFlow flow, JsonPointer pointer) {
        Set<String> scopes = Optional.<Map<String, String>>ofNullable(flow.getScopes()).map(Map::keySet).orElse(Set.of());
        List<String> urls = Stream.of(flow.getAuthorizationUrl(), flow.getTokenUrl(), flow.getRefreshUrl())
                .filter(Objects::nonNull)
                .toList();
        return new ApiOAuthFlow(scheme, type, pointer, scopes, urls);
    }
}
