// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OAuthRulesTest {

    @Nested
    class OAuthHasNoImplicitFlowTest {

        private final OAuthHasNoImplicitFlow rule = new OAuthHasNoImplicitFlow();

        @Test
        void passesTheAuthorizationCodeFlow() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      securitySchemes:
                        login:
                          type: oauth2
                          flows:
                            authorizationCode: {authorizationUrl: 'https://sso.example.com/authorize', tokenUrl: 'https://sso.example.com/token', scopes: {}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsTheImplicitFlow() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      securitySchemes:
                        sso:
                          type: oauth2
                          flows:
                            implicit: {authorizationUrl: 'https://sso.example.com/authorize', scopes: {}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/securitySchemes/sso/flows/implicit");
        }
    }

    @Nested
    class OAuthScopesAreDeclaredTest {

        private final OAuthScopesAreDeclared rule = new OAuthScopesAreDeclared();

        @Test
        void passesScopesThatAnyFlowOfTheSchemeDeclares() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    security: [{login: [orders:read]}]
                    paths:
                      /orders:
                        post:
                          security: [{login: [orders:write]}]
                          responses: {'201': {description: Created}}
                    components:
                      securitySchemes:
                        login:
                          type: oauth2
                          flows:
                            authorizationCode:
                              authorizationUrl: 'https://sso.example.com/authorize'
                              tokenUrl: 'https://sso.example.com/token'
                              scopes: {orders:read: Read orders}
                            clientCredentials:
                              tokenUrl: 'https://sso.example.com/token'
                              scopes: {orders:write: Change orders}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsAScopeNoFlowDeclares() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        post:
                          security: [{login: [orders:read, ordres:write]}]
                          responses: {'201': {description: Created}}
                    components:
                      securitySchemes:
                        login:
                          type: oauth2
                          flows:
                            clientCredentials: {tokenUrl: 'https://sso.example.com/token', scopes: {orders:read: Read orders}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1orders/post/security/0");
            assertThat(result.messages())
                    .containsExactly("Requires scopes the security scheme does not declare: ordres:write of login.");
        }

        @Test
        void doesNotApplyToOpenIdConnectOrToRequirementsWithoutScopes() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    security: [{oidc: [orders:read]}, {login: []}]
                    paths: {}
                    components:
                      securitySchemes:
                        oidc: {type: openIdConnect, openIdConnectUrl: 'https://sso.example.com/.well-known/openid-configuration'}
                        login:
                          type: oauth2
                          flows:
                            clientCredentials: {tokenUrl: 'https://sso.example.com/token', scopes: {}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }

    @Nested
    class OAuthUrlsUseHttpsTest {

        private final OAuthUrlsUseHttps rule = new OAuthUrlsUseHttps();

        @Test
        void passesHttpsAndALocalIdentityProvider() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      securitySchemes:
                        login:
                          type: oauth2
                          flows:
                            authorizationCode: {authorizationUrl: 'https://sso.example.com/authorize', tokenUrl: 'https://sso.example.com/token', scopes: {}}
                            clientCredentials: {tokenUrl: 'http://localhost:9000/token', scopes: {}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsAFlowWithARemotePlainHttpUrl() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      securitySchemes:
                        login:
                          type: oauth2
                          flows:
                            authorizationCode:
                              authorizationUrl: 'https://sso.example.com/authorize'
                              tokenUrl: 'http://sso.example.com/token'
                              refreshUrl: 'http://sso.example.com/refresh'
                              scopes: {}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/securitySchemes/login/flows/authorizationCode");
            assertThat(result.messages()).first().asString()
                    .contains("http://sso.example.com/token, http://sso.example.com/refresh");
        }
    }
}
