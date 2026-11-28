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
 * BQ-SEC-014: an OAuth flow's authorization, token and refresh URLs use {@code https}. Those requests carry
 * authorization codes, client secrets and tokens, and OAuth 2.0 requires TLS for them. Loopback hosts are exempt for
 * local identity providers.
 */
public final class OAuthUrlsUseHttps extends MajorSecurityRule<ApiOAuthFlow> {

    @Override
    public String id() {
        return "BQ-SEC-014";
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
        List<String> plain = flow.urls().stream().filter(PlainHttpUrl::reachesAnotherHost).toList();
        return plain.isEmpty() ? Optional.empty() : Optional.of(overPlainHttp(plain));
    }

    private static Explanation overPlainHttp(List<String> urls) {
        return new Explanation(
                "OAuth flow sends codes, secrets or tokens over plain http: " + String.join(", ", urls) + ".",
                "Point authorizationUrl, tokenUrl and refreshUrl in @OAuthFlow at the identity provider's https "
                        + "endpoints.");
    }
}
