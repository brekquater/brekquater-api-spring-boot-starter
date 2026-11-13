// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InfoRulesTest {

    private static final String DESCRIBED_API = """
            openapi: 3.1.0
            info:
              title: Orders API
              version: "1.4"
              description: Places and tracks orders.
              contact: {name: Orders team, email: orders@example.com}
            paths: {}
            """;

    private static final String BARE_API = """
            openapi: 3.1.0
            info: {title: OpenAPI definition, version: v0}
            paths: {}
            """;

    @Nested
    class ApiDeclaresContactTest {

        private final ApiDeclaresContact rule = new ApiDeclaresContact();

        @Test
        void passesAnApiWithAContact() {
            RuleFixture result = RuleFixture.evaluate(rule, DESCRIBED_API);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsAnApiWithoutAContact() {
            assertThat(RuleFixture.evaluate(rule, BARE_API).failingLocations()).containsExactly("/info");
        }

        @Test
        void reportsAContactWithOnlyBlankValues() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0", contact: {name: " ", email: ""}}
                    paths: {}
                    """);

            assertThat(result.failingLocations()).containsExactly("/info");
        }

        @Test
        void passesAContactThatGivesOnlyAUrl() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0", contact: {url: 'https://example.com/support'}}
                    paths: {}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }
    }

    @Nested
    class ApiHasDescriptionTest {

        private final ApiHasDescription rule = new ApiHasDescription();

        @Test
        void passesAnApiWithADescription() {
            assertThat(RuleFixture.evaluate(rule, DESCRIBED_API).failingLocations()).isEmpty();
        }

        @Test
        void reportsAnApiWithoutADescription() {
            assertThat(RuleFixture.evaluate(rule, BARE_API).failingLocations()).containsExactly("/info");
        }

        @Test
        void reportsAWhitespaceOnlyDescription() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0", description: "  "}
                    paths: {}
                    """);

            assertThat(result.failingLocations()).hasSize(1);
        }
    }

    @Nested
    class ApiInfoIsNotPlaceholderTest {

        private final ApiInfoIsNotPlaceholder rule = new ApiInfoIsNotPlaceholder();

        @Test
        void passesARealTitleAndVersion() {
            assertThat(RuleFixture.evaluate(rule, DESCRIBED_API).failingLocations()).isEmpty();
        }

        @Test
        void reportsSpringdocsPlaceholders() {
            assertThat(RuleFixture.evaluate(rule, BARE_API).failingLocations()).containsExactly("/info");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "{title: Orders API, version: v0}",
            "{title: OpenAPI definition, version: '1.0'}",
            "{title: ' ', version: '1.0'}"
        })
        void reportsAPlaceholderOrBlankTitleOrVersion(String info) {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: %s
                    paths: {}
                    """.formatted(info));

            assertThat(result.failingLocations()).containsExactly("/info");
        }

        @Test
        void reportsADocumentWithoutInfo() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    paths: {}
                    """);

            assertThat(result.failingLocations()).containsExactly("/info");
        }
    }
}
