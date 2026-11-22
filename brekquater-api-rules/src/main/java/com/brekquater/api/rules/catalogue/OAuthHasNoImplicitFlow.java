// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiOAuthFlow;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import java.util.List;
import java.util.Optional;

/**
 * BQ-SEC-012: no OAuth 2.0 scheme offers the implicit flow. It returns the access token in the redirect URL, where
 * it can leak or be injected, and the OAuth 2.0 Security Best Current Practice (RFC 9700) says clients should not
 * use it.
 */
public final class OAuthHasNoImplicitFlow extends MajorSecurityRule<ApiOAuthFlow> {

    private static final String IMPLICIT = "implicit";
    private static final Explanation IMPLICIT_FLOW = new Explanation(
            "OAuth scheme offers the implicit flow, which returns the token in the URL; RFC 9700 says not to use it.",
            "Offer the authorization code flow instead, and turn on PKCE for Swagger UI with "
                    + "springdoc.swagger-ui.oauth.use-pkce-with-authorization-code-grant=true.");

    @Override
    public String id() {
        return "BQ-SEC-012";
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
        return flow.type().equals(IMPLICIT) ? Optional.of(IMPLICIT_FLOW) : Optional.empty();
    }
}
