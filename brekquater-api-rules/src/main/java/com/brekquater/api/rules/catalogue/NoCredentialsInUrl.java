// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.ApiParameter;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * BQ-SEC-005: no path or query parameter carries a password, secret, token or API key. Anything in the URL ends up
 * in logs and browser history; RFC 9700 says access tokens must not be passed in a query parameter.
 *
 * <p>Names are compared exactly after removing case, {@code _} and {@code -}, so {@code access_token} matches but
 * {@code pageToken}, a pagination cursor, does not.
 */
public final class NoCredentialsInUrl extends MajorSecurityRule<ApiOperation> {

    private static final Set<String> IN_URL = Set.of("path", "query");
    private static final Set<String> CREDENTIALS = Set.of(
            "password", "passwd", "pwd", "secret", "clientsecret", "token", "accesstoken", "refreshtoken", "idtoken",
            "apikey");

    @Override
    public String id() {
        return "BQ-SEC-005";
    }

    @Override
    protected List<ApiOperation> targets(OpenApiContext document) {
        return document.operations().stream()
                .filter(operation -> operation.parameters().stream().anyMatch(NoCredentialsInUrl::inUrl))
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiOperation operation) {
        return operation.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiOperation operation, OpenApiContext document) {
        List<String> credentials = operation.parameters().stream()
                .filter(NoCredentialsInUrl::inUrl)
                .map(ApiParameter::name)
                .filter(NoCredentialsInUrl::namesCredential)
                .toList();
        return credentials.isEmpty() ? Optional.empty() : Optional.of(inUrl(credentials));
    }

    private static boolean inUrl(ApiParameter parameter) {
        return IN_URL.contains(parameter.location());
    }

    private static boolean namesCredential(String name) {
        return CREDENTIALS.contains(name.toLowerCase(Locale.ROOT).replace("_", "").replace("-", ""));
    }

    private static Explanation inUrl(List<String> credentials) {
        return new Explanation(
                "Credential sent in the URL: " + String.join(", ", credentials) + ".",
                "Send tokens in the Authorization header and other secrets in the request body, and read them with "
                        + "@RequestHeader or @RequestBody instead of @RequestParam.");
    }
}
