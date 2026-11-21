// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiOAuthFlow;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/**
 * BQ-SEC-010: no OAuth 2.0 scheme offers the resource owner password flow. It hands the user's password to the
 * client, and the OAuth 2.0 Security Best Current Practice (RFC 9700) says it must not be used.
 */
public final class OAuthHasNoPasswordFlow extends OpenApiRule<ApiOAuthFlow> {

    private static final String PASSWORD = "password";
    private static final Explanation PASSWORD_FLOW = new Explanation(
            "OAuth scheme offers the resource owner password flow, which RFC 9700 says must not be used.",
            "Offer the authorization code flow (with PKCE) for users, or client credentials for services: "
                    + "@OAuthFlows(authorizationCode = @OAuthFlow(...)).");

    @Override
    public String id() {
        return "BQ-SEC-010";
    }

    @Override
    public Severity severity() {
        return Severity.BLOCKER;
    }

    @Override
    public Category category() {
        return Category.SECURITY;
    }

    @Override
    protected List<ApiOAuthFlow> targets(OpenApiContext document) {
        return document.oauthFlows();
    }

    @Override
    protected JsonPointer locate(ApiOAuthFlow flow) {
        return flow.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiOAuthFlow flow, OpenApiContext document) {
        return flow.type().equals(PASSWORD) ? Optional.of(PASSWORD_FLOW) : Optional.empty();
    }
}
