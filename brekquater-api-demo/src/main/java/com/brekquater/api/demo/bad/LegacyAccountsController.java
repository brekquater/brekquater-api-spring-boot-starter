// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo.bad;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * An accounts API with the security mistakes the rules look for. Each scheme and method is commented with the
 * rules it breaks. Do not copy it.
 */
@Profile("bad")
@RestController
// BQ-SEC-003: HTTP Basic sends the password with every request.
@SecurityScheme(name = "legacyBasic", type = SecuritySchemeType.HTTP, scheme = "basic")
// BQ-SEC-004: an API key in the query string ends up in logs and browser history.
@SecurityScheme(name = "legacyKey", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.QUERY, paramName = "api_key")
// BQ-SEC-015: Negotiate is not an API authentication scheme.
@SecurityScheme(name = "legacyKerberos", type = SecuritySchemeType.HTTP, scheme = "negotiate")
// BQ-SEC-016: "JWT" is a token format, not a registered HTTP scheme; it belongs in bearerFormat.
@SecurityScheme(name = "legacyToken", type = SecuritySchemeType.HTTP, scheme = "JWT")
// BQ-SEC-012: the implicit flow returns tokens in the URL. BQ-SEC-014: over plain http.
@SecurityScheme(
        name = "legacySso",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(implicit = @OAuthFlow(
                authorizationUrl = "http://sso.example.com/authorize",
                scopes = @OAuthScope(name = "accounts:read", description = "Read accounts"))))
class LegacyAccountsController {

    private static final LegacyAccount ACCOUNT = new LegacyAccount("ada", "correct-horse");

    // BQ-SEC-003: Basic is the only way in. BQ-SEC-005: an access token in the query string.
    @GetMapping("/accounts")
    @SecurityRequirement(name = "legacyBasic")
    List<LegacyAccount> list(@RequestParam("access_token") String accessToken) {
        return List.of(ACCOUNT);
    }

    // BQ-SEC-013: "accounts:write" is not a scope the scheme declares. BQ-SEC-017: the password is readable.
    @GetMapping("/accounts/{id}")
    @SecurityRequirement(name = "legacySso", scopes = "accounts:write")
    LegacyAccount find(@PathVariable String id) {
        return ACCOUNT;
    }

    /**
     * An account as the legacy API returns it, password included.
     *
     * @param login the user name
     * @param password the user's password
     */
    record LegacyAccount(String login, String password) {}
}
