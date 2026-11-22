// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.ApiSecurityRequirement;
import com.brekquater.api.rules.ApiSecurityScheme;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BQ-SEC-003: HTTP Basic is not the only way to call an operation. Basic sends the password itself with every
 * request, so it cannot be scoped, expire or require a second factor. Basic offered next to a token scheme is not
 * reported: callers can choose the stronger one.
 */
public final class BasicIsNotTheOnlyLogin extends MajorSecurityRule<ApiOperation> {

    private static final String BASIC = "basic";
    private static final Explanation ONLY_BASIC = new Explanation(
            "HTTP Basic is the only way to call this operation, so every request carries the user's password.",
            "Accept bearer tokens instead, for example with Spring Security's OAuth 2.0 resource server, and declare "
                    + "@SecurityScheme(type = SecuritySchemeType.HTTP, scheme = \"bearer\").");

    @Override
    public String id() {
        return "BQ-SEC-003";
    }

    @Override
    protected List<ApiOperation> targets(OpenApiContext document) {
        Set<String> basic = basicSchemes(document);
        return document.operations().stream()
                .filter(operation -> effectiveSecurity(operation, document).stream()
                        .anyMatch(requirement -> requirement.schemeNames().stream().anyMatch(basic::contains)))
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiOperation operation) {
        return operation.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiOperation operation, OpenApiContext document) {
        Set<String> basic = basicSchemes(document);
        boolean onlyBasic = effectiveSecurity(operation, document).stream()
                .filter(requirement -> !requirement.allowsAnonymousAccess())
                .allMatch(requirement -> basic.containsAll(requirement.schemeNames()));
        return onlyBasic ? Optional.of(ONLY_BASIC) : Optional.empty();
    }

    private static List<ApiSecurityRequirement> effectiveSecurity(ApiOperation operation, OpenApiContext document) {
        return operation.declaredSecurity().orElse(document.rootSecurity());
    }

    private static Set<String> basicSchemes(OpenApiContext document) {
        return document.securitySchemes().stream()
                .filter(scheme -> scheme.usesHttpScheme(BASIC))
                .map(ApiSecurityScheme::name)
                .collect(Collectors.toUnmodifiableSet());
    }
}
