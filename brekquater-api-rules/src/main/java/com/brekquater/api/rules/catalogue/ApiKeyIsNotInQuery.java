// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiSecurityScheme;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import java.util.List;
import java.util.Optional;

/**
 * BQ-SEC-004: an API key is not sent in the query string. URLs end up in server and proxy logs, browser history
 * and {@code Referer} headers, and the key with them.
 */
public final class ApiKeyIsNotInQuery extends MajorSecurityRule<ApiSecurityScheme> {

    private static final String QUERY = "query";
    private static final Explanation KEY_IN_URL = new Explanation(
            "API key is sent in the query string, where logs and browser history keep it.",
            "Send the key in a header: @SecurityScheme(type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, "
                    + "paramName = \"X-API-Key\").");

    @Override
    public String id() {
        return "BQ-SEC-004";
    }

    @Override
    protected List<ApiSecurityScheme> targets(OpenApiContext document) {
        return document.securitySchemes().stream().filter(ApiSecurityScheme::isApiKey).toList();
    }

    @Override
    protected JsonPointer locate(ApiSecurityScheme scheme) {
        return scheme.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiSecurityScheme scheme, OpenApiContext document) {
        return scheme.apiKeyLocation().filter(QUERY::equals).isPresent() ? Optional.of(KEY_IN_URL) : Optional.empty();
    }
}
