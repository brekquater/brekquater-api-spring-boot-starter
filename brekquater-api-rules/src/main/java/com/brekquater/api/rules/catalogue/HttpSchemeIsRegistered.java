// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiSecurityScheme;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * BQ-SEC-016: an {@code http} security scheme names an HTTP authentication scheme from the IANA registry. Clients
 * and Swagger UI only know how to send registered ones; {@code scheme: JWT}, a common mistake, is a token format that
 * belongs in {@code bearerFormat}.
 */
public final class HttpSchemeIsRegistered extends OpenApiRule<ApiSecurityScheme> {

    // The IANA HTTP Authentication Scheme Registry, as read on 2026-11-21. Names are compared ignoring case.
    private static final Set<String> REGISTERED = Set.of(
            "basic", "bearer", "concealed", "digest", "dpop", "gnap", "hoba", "mutual", "negotiate", "oauth",
            "privatetoken", "scram-sha-1", "scram-sha-256", "vapid", "iso-kam3-dl-2048-sha256",
            "iso-kam3-dl-4096-sha512", "iso-kam3-ec-p256-sha256", "iso-kam3-ec-p521-sha512");
    private static final Explanation UNREGISTERED = new Explanation(
            "Security scheme names an HTTP authentication scheme that is not registered, so clients cannot send it.",
            "For JSON Web Tokens, declare @SecurityScheme(type = SecuritySchemeType.HTTP, scheme = \"bearer\", "
                    + "bearerFormat = \"JWT\").");

    @Override
    public String id() {
        return "BQ-SEC-016";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public Category category() {
        return Category.SECURITY;
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
        boolean registered = REGISTERED.stream().anyMatch(scheme::usesHttpScheme);
        return registered ? Optional.empty() : Optional.of(UNREGISTERED);
    }
}
