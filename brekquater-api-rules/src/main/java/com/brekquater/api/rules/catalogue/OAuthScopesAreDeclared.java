// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiOAuthFlow;
import com.brekquater.api.rules.ApiSecurityRequirement;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BQ-SEC-013: every scope an operation requires of an OAuth 2.0 scheme is declared by that scheme. An undeclared
 * scope is usually a typo, and a token can never carry it, so the operation either rejects every caller or the check
 * behind it is not the one documented. OpenID Connect schemes are not checked: their scopes are published by the
 * provider, and the library makes no network calls.
 */
public final class OAuthScopesAreDeclared extends MajorSecurityRule<ApiSecurityRequirement> {

    @Override
    public String id() {
        return "BQ-SEC-013";
    }

    @Override
    protected List<ApiSecurityRequirement> targets(OpenApiContext document) {
        Set<String> oauthSchemes = oauthSchemes(document);
        return DocumentTargets.securityRequirements(document).stream()
                .filter(requirement -> requirement.scopes().entrySet().stream()
                        .anyMatch(entry -> oauthSchemes.contains(entry.getKey()) && !entry.getValue().isEmpty()))
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiSecurityRequirement requirement) {
        return requirement.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiSecurityRequirement requirement, OpenApiContext document) {
        Set<String> oauthSchemes = oauthSchemes(document);
        List<String> undeclared = requirement.scopes().entrySet().stream()
                .filter(entry -> oauthSchemes.contains(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream()
                        .filter(scope -> !declaredScopes(document, entry.getKey()).contains(scope))
                        .map(scope -> scope + " of " + entry.getKey()))
                .toList();
        return undeclared.isEmpty() ? Optional.empty() : Optional.of(undeclared(undeclared));
    }

    private static Set<String> oauthSchemes(OpenApiContext document) {
        return document.oauthFlows().stream().map(ApiOAuthFlow::scheme).collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> declaredScopes(OpenApiContext document, String scheme) {
        return document.oauthFlows().stream()
                .filter(flow -> flow.scheme().equals(scheme))
                .flatMap(flow -> flow.scopes().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Explanation undeclared(List<String> scopes) {
        return new Explanation(
                "Requires scopes the security scheme does not declare: " + String.join(", ", scopes) + ".",
                "Fix the scope name in @SecurityRequirement(scopes = ...), or declare it with @OAuthScope in the "
                        + "scheme's @OAuthFlow.");
    }
}
