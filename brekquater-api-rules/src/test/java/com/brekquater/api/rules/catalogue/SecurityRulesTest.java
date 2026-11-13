// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SecurityRulesTest {

    @Nested
    class OperationDeclaresSecurityTest {

        private final OperationDeclaresSecurity rule = new OperationDeclaresSecurity();

        @Test
        void passesOperationsThatInheritTheDocumentsSecurity() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    security: [{bearer: []}]
                    paths:
                      /orders:
                        get: {responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void passesAnOperationThatDeclaresItsOwnSecurity() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get: {security: [{apiKey: []}], responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void reportsAnOperationWithNoSecurityAnywhere() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get: {responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1orders/get");
        }

        @Test
        void reportsAnOperationThatOptsOutOfTheDocumentsSecurity() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    security: [{bearer: []}]
                    paths:
                      /orders:
                        get: {security: [], responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1orders/get");
        }

        @Test
        void reportsSecurityThatAlsoAllowsAnonymousCallers() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get: {security: [{bearer: []}, {}], responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).hasSize(1);
        }

        @ParameterizedTest
        @ValueSource(strings = {"true", "'true'", "'TRUE'"})
        void passesAnOperationMarkedPublic(String marker) {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /health:
                        get: {x-brekquater-public: %s, responses: {'200': {description: OK}}}
                    """.formatted(marker));

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void reportsAnOperationMarkedPublicWithFalse() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /health:
                        get: {x-brekquater-public: false, responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).hasSize(1);
        }
    }

    @Nested
    class SecurityRequirementNamesDefinedSchemeTest {

        private final SecurityRequirementNamesDefinedScheme rule = new SecurityRequirementNamesDefinedScheme();

        @Test
        void passesRequirementsThatNameDefinedSchemes() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    security: [{bearer: []}]
                    paths:
                      /orders:
                        get: {security: [{bearer: [], apiKey: []}], responses: {'200': {description: OK}}}
                    components:
                      securitySchemes:
                        bearer: {type: http, scheme: bearer}
                        apiKey: {type: apiKey, in: header, name: X-API-Key}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsRequirementsThatNameAnUndefinedScheme() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    security: [{bearer: []}]
                    paths:
                      /orders:
                        get: {security: [{apiKey: []}, {oauth: []}], responses: {'200': {description: OK}}}
                    components:
                      securitySchemes:
                        apiKey: {type: apiKey, in: header, name: X-API-Key}
                    """);

            assertThat(result.failingLocations()).containsExactly("/security/0", "/paths/~1orders/get/security/1");
            assertThat(result.messages()).first().asString().contains("[bearer]");
        }

        @Test
        void passesTheAnonymousEntry() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get: {security: [{}], responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }
    }
}
