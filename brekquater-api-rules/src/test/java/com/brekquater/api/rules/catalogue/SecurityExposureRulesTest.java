// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SecurityExposureRulesTest {

    @Nested
    class OAuthHasNoPasswordFlowTest {

        private final OAuthHasNoPasswordFlow rule = new OAuthHasNoPasswordFlow();

        @Test
        void passesTheAuthorizationCodeAndClientCredentialsFlows() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      securitySchemes:
                        login:
                          type: oauth2
                          flows:
                            authorizationCode: {authorizationUrl: 'https://auth.example.com/authorize', tokenUrl: 'https://auth.example.com/token', scopes: {}}
                            clientCredentials: {tokenUrl: 'https://auth.example.com/token', scopes: {}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsThePasswordFlow() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      securitySchemes:
                        legacyLogin:
                          type: oauth2
                          flows:
                            password: {tokenUrl: 'https://auth.example.com/token', scopes: {}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/securitySchemes/legacyLogin/flows/password");
        }

        @Test
        void doesNotApplyToOtherKindsOfScheme() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      securitySchemes:
                        bearer: {type: http, scheme: bearer}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }

    @Nested
    class NoSensitiveActuatorEndpointTest {

        private final NoSensitiveActuatorEndpoint rule = new NoSensitiveActuatorEndpoint();

        @Test
        void passesHealthInfoAndTheActuatorIndex() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /actuator: {get: {responses: {'200': {description: OK}}}}
                      /actuator/health: {get: {responses: {'200': {description: OK}}}}
                      /actuator/info: {get: {responses: {'200': {description: OK}}}}
                      /orders: {get: {responses: {'200': {description: OK}}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(4);
        }

        @Test
        void reportsEveryOtherEndpointAndItsSubPaths() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /actuator/env: {get: {responses: {'200': {description: OK}}}}
                      /actuator/env/{toMatch}: {get: {responses: {'200': {description: OK}}}}
                      /manage/actuator/heapdump: {get: {responses: {'200': {description: OK}}}}
                    """);

            assertThat(result.failingLocations()).containsExactly(
                    "/paths/~1actuator~1env", "/paths/~1actuator~1env~1{toMatch}", "/paths/~1manage~1actuator~1heapdump");
            assertThat(result.messages()).first().isEqualTo("Path exposes the Actuator endpoint 'env'.");
        }
    }
}
