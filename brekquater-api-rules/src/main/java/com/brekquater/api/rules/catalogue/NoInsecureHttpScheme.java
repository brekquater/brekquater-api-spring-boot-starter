// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiSecurityScheme;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * BQ-SEC-015: no {@code http} security scheme uses {@code negotiate} (Kerberos or NTLM over SPNEGO) or
 * {@code oauth} (OAuth 1.0). Both are built for other settings than public APIs, and OAuth 1.0 is superseded by
 * OAuth 2.0.
 */
public final class NoInsecureHttpScheme extends MajorSecurityRule<ApiSecurityScheme> {

    private static final Set<String> INSECURE = Set.of("negotiate", "oauth");
    private static final Explanation INSECURE_SCHEME = new Explanation(
            "Security scheme uses Negotiate or OAuth 1.0, which are not suited to authenticating API callers.",
            "Use bearer tokens: @SecurityScheme(type = SecuritySchemeType.HTTP, scheme = \"bearer\"), backed by "
                    + "Spring Security's OAuth 2.0 resource server.");

    @Override
    public String id() {
        return "BQ-SEC-015";
    }

    @Override
    protected List<ApiSecurityScheme> targets(OpenApiContext document) {
        return document.securitySchemes().stream().filter(ApiSecurityScheme::isHttp).toList();
    }

    @Override
    protected JsonPointer locate(ApiSecurityScheme scheme) {
        return scheme.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiSecurityScheme scheme, OpenApiContext document) {
        boolean insecure = INSECURE.stream().anyMatch(scheme::usesHttpScheme);
        return insecure ? Optional.of(INSECURE_SCHEME) : Optional.empty();
    }
}
