// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AuthenticationRulesTest {

    @Nested
    class BasicIsNotTheOnlyLoginTest {

        private final BasicIsNotTheOnlyLogin rule = new BasicIsNotTheOnlyLogin();

        @Test
        void passesBasicOfferedNextToABearerToken() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          security: [{basicAuth: []}, {bearer: []}]
                          responses: {'200': {description: OK}}
                    components:
                      securitySchemes:
                        basicAuth: {type: http, scheme: basic}
                        bearer: {type: http, scheme: bearer}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsAnOperationThatOnlyAcceptsBasicIncludingThroughTheDocumentWideSecurity() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    security: [{basicAuth: []}]
                    paths:
                      /orders:
                        get: {responses: {'200': {description: OK}}}
                      /orders/{id}:
                        get:
                          security: [{BasicAuth: []}]
                          responses: {'200': {description: OK}}
                    components:
                      securitySchemes:
                        basicAuth: {type: http, scheme: basic}
                        BasicAuth: {type: http, scheme: Basic}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1orders/get", "/paths/~1orders~1{id}/get");
        }

        @Test
        void doesNotApplyWhereBasicIsNotOffered() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    security: [{bearer: []}]
                    paths:
                      /orders:
                        get: {responses: {'200': {description: OK}}}
                    components:
                      securitySchemes:
                        bearer: {type: http, scheme: bearer}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }

    @Nested
    class ApiKeyIsNotInQueryTest {

        private final ApiKeyIsNotInQuery rule = new ApiKeyIsNotInQuery();

        @Test
        void passesAKeySentInAHeader() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      securitySchemes:
                        key: {type: apiKey, in: header, name: X-API-Key}
                        bearer: {type: http, scheme: bearer}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsAKeySentInTheQueryString() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      securitySchemes:
                        key: {type: apiKey, in: query, name: api_key}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/securitySchemes/key");
        }
    }

    @Nested
    class NoCredentialsInUrlTest {

        private final NoCredentialsInUrl rule = new NoCredentialsInUrl();

        @Test
        void passesOrdinaryParametersAndAPaginationCursor() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          parameters:
                            - {name: pageToken, in: query}
                            - {name: Authorization, in: header}
                          responses: {'200': {description: OK}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsCredentialsInTheQueryOrThePathWhateverTheirSpelling() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /accounts:
                        get:
                          parameters:
                            - {name: access_token, in: query}
                            - {name: API-Key, in: query}
                          responses: {'200': {description: OK}}
                      /reset/{password}:
                        post:
                          parameters: [{name: password, in: path, required: true}]
                          responses: {'204': {description: No Content}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1accounts/get", "/paths/~1reset~1{password}/post");
            assertThat(result.messages()).first().isEqualTo("Credential sent in the URL: access_token, API-Key.");
        }

        @Test
        void doesNotApplyToAnOperationWithoutUrlParameters() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /login:
                        post:
                          parameters: [{name: password, in: header}]
                          responses: {'204': {description: No Content}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }

    @Nested
    class ServerUsesHttpsTest {

        private final ServerUsesHttps rule = new ServerUsesHttps();

        @Test
        void passesHttpsRelativeAndLoopbackServers() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    servers:
                      - {url: 'https://api.example.com'}
                      - {url: /api}
                      - {url: 'http://localhost:8080'}
                      - {url: 'http://127.0.0.1:8080'}
                      - {url: '{scheme}://api.example.com', variables: {scheme: {default: https}}}
                    paths: {}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(5);
        }

        @Test
        void reportsARemoteServerOverPlainHttp() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    servers:
                      - {url: 'https://api.example.com'}
                      - {url: 'http://api.example.com', description: Production}
                    paths: {}
                    """);

            assertThat(result.failingLocations()).containsExactly("/servers/1");
        }

        @Test
        void ignoresTheServerSpringdocBuildsFromTheRequest() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    servers:
                      - {url: 'http://192.168.1.20:8080', description: Generated server url}
                    paths: {}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }

    @Nested
    class NoInsecureHttpSchemeTest {

        private final NoInsecureHttpScheme rule = new NoInsecureHttpScheme();

        @Test
        void passesBearerAndDigest() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      securitySchemes:
                        bearer: {type: http, scheme: bearer}
                        digest: {type: http, scheme: digest}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsNegotiateAndOAuth1() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      securitySchemes:
                        kerberos: {type: http, scheme: Negotiate}
                        legacy: {type: http, scheme: oauth}
                    """);

            assertThat(result.failingLocations())
                    .containsExactly("/components/securitySchemes/kerberos", "/components/securitySchemes/legacy");
        }
    }

    @Nested
    class HttpSchemeIsRegisteredTest {

        private final HttpSchemeIsRegistered rule = new HttpSchemeIsRegistered();

        @Test
        void passesRegisteredSchemesWhateverTheirCase() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      securitySchemes:
                        bearer: {type: http, scheme: Bearer, bearerFormat: JWT}
                        dpop: {type: http, scheme: DPoP}
                        key: {type: apiKey, in: header, name: X-API-Key}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsATokenFormatUsedAsTheScheme() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      securitySchemes:
                        jwt: {type: http, scheme: JWT}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/securitySchemes/jwt");
        }
    }

    @Nested
    class SecretIsNotReturnedTest {

        private final SecretIsNotReturned rule = new SecretIsNotReturned();

        @Test
        void passesAWriteOnlyPasswordAndASecretThatIsOnlySent() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Accounts API, version: "1.0"}
                    paths:
                      /accounts:
                        post:
                          requestBody: {content: {application/json: {schema: {$ref: '#/components/schemas/NewAccount'}}}}
                          responses:
                            '201': {description: Created, content: {application/json: {schema: {$ref: '#/components/schemas/Account'}}}}
                    components:
                      schemas:
                        NewAccount: {type: object, properties: {password: {type: string}}}
                        Account: {type: object, properties: {login: {type: string}, password: {type: string, writeOnly: true}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsASecretInlineOrInASchemaAResponseReachesThroughOthers() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Accounts API, version: "1.0"}
                    paths:
                      /accounts/{id}:
                        get:
                          responses:
                            '200': {description: OK, content: {application/json: {schema: {$ref: '#/components/schemas/Account'}}}}
                      /clients/{id}:
                        get:
                          responses:
                            '200':
                              description: OK
                              content:
                                application/json:
                                  schema: {type: object, properties: {client_secret: {type: string}}}
                    components:
                      schemas:
                        Account: {type: object, properties: {credentials: {$ref: '#/components/schemas/Credentials'}}}
                        Credentials: {type: object, properties: {Password: {type: string}}}
                    """);

            assertThat(result.failingLocations()).containsExactly(
                    "/components/schemas/Credentials/properties/Password",
                    "/paths/~1clients~1{id}/get/responses/200/content/application~1json/schema/properties/client_secret");
        }

        @Test
        void leavesTokensAlone() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Accounts API, version: "1.0"}
                    paths:
                      /token:
                        post:
                          responses:
                            '200':
                              description: OK
                              content:
                                application/json:
                                  schema: {type: object, properties: {access_token: {type: string}}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }
}
