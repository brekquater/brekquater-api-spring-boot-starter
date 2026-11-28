// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.OpenApiRule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class VersioningRulesTest {

    private static RuleFixture paths(OpenApiRule<?> rule, String paths) {
        return RuleFixture.evaluate(rule, """
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0.0"}
                paths:
                """ + paths.indent(2));
    }

    private static RuleFixture version(String version) {
        return RuleFixture.evaluate(new InfoVersionIsSemantic(), """
                openapi: 3.1.0
                info: {title: Orders API, version: '%s'}
                paths: {}
                """.formatted(version));
    }

    @Nested
    class InfoVersionIsSemanticTest {

        @Test
        void passesSemanticVersionsAndDates() {
            assertThat(version("1.4.0").failingLocations()).isEmpty();
            assertThat(version("0.2.0-SNAPSHOT").failingLocations()).isEmpty();
            assertThat(version("2.0.0-rc.1+build.5").failingLocations()).isEmpty();
            assertThat(version("2026-11-27").failingLocations()).isEmpty();
            assertThat(version("2026-11-27-preview").applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsVersionsWithoutThreeNumbers() {
            assertThat(version("1.0").failingLocations()).containsExactly("/info");
            assertThat(version("v2").failingLocations()).containsExactly("/info");
            assertThat(version("1.02.0").failingLocations()).containsExactly("/info");
            assertThat(version("latest").messages()).containsExactly("API version latest is neither MAJOR.MINOR.PATCH nor a date.");
        }

        @Test
        void leavesSpringdocsPlaceholderToTheRuleThatReportsIt() {
            assertThat(version("v0").applicableTargets()).isZero();
            assertThat(version(" ").applicableTargets()).isZero();
        }
    }

    @Nested
    class OneVersioningMechanismTest {

        private final OneVersioningMechanism rule = new OneVersioningMechanism();

        @Test
        void passesAnApiThatAlwaysUsesTheSameHeaderAndIgnoresUnversionedOperations() {
            RuleFixture result = paths(rule, """
                    /orders:
                      get:
                        parameters: [{name: X-API-Version, in: header, schema: {type: string, enum: ['1', '2']}}]
                        responses: {'200': {description: OK}}
                    /customers:
                      get:
                        parameters: [{name: X-API-Version, in: header}]
                        responses: {'200': {description: OK}}
                    /health:
                      get: {responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsOperationsThatUseAnotherMechanismThanMostOperations() {
            RuleFixture result = paths(rule, """
                    /v1/orders:
                      get: {responses: {'200': {description: OK}}}
                    /v1/customers:
                      get: {responses: {'200': {description: OK}}}
                    /invoices:
                      get:
                        parameters: [{name: api-version, in: query}]
                        responses: {'200': {description: OK}}
                    /v1/payments:
                      get:
                        parameters: [{name: Accept-Version, in: header}]
                        responses: {'200': {description: OK}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1invoices/get", "/paths/~1v1~1payments/get");
            assertThat(result.messages()).first().isEqualTo(
                    "Operation receives its version through a query parameter, while most versioned operations use "
                            + "a path segment.");
        }

        @Test
        void doesNotMistakeACookieOrAnIdentifierInThePathForAVersion() {
            RuleFixture result = paths(rule, """
                    /orders/1234567:
                      get:
                        parameters: [{name: version, in: cookie}]
                        responses: {'200': {description: OK}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }

    @Nested
    class VersionSegmentIsMajorOnlyTest {

        private final VersionSegmentIsMajorOnly rule = new VersionSegmentIsMajorOnly();

        @Test
        void passesMajorVersionsChannelsAndSpringsNumberedSegments() {
            RuleFixture result = paths(rule, """
                    /v1/orders: {}
                    /v2beta/orders: {}
                    /api/3/orders: {}
                    /orders/{version}: {}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(3);
        }

        @Test
        void reportsAMinorOrPatchVersionInThePath() {
            RuleFixture result = paths(rule, """
                    /v1.2/orders: {}
                    /api/2.0.1/orders: {}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1api~12.0.1~1orders", "/paths/~1v1.2~1orders");
            assertThat(result.messages()).contains("Path version v1.2 includes a minor version.");
        }
    }

    @Nested
    class VersionSegmentsShareOneIndexTest {

        private final VersionSegmentsShareOneIndex rule = new VersionSegmentsShareOneIndex();

        @Test
        void passesVersionsAtTheSamePosition() {
            RuleFixture result = paths(rule, """
                    /api/v1/orders: {}
                    /api/v2/customers/{id}: {}
                    /health: {}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsAVersionAtAnotherPositionThanMost() {
            RuleFixture result = paths(rule, """
                    /api/v1/orders: {}
                    /api/v1/customers: {}
                    /v2/invoices: {}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1v2~1invoices");
            assertThat(result.messages()).containsExactly(
                    "Path version v2 is segment 0, while most versioned paths have it at segment 1.");
        }
    }

    @Nested
    class OlderVersionsAreDeprecatedTest {

        private final OlderVersionsAreDeprecated rule = new OlderVersionsAreDeprecated();

        @Test
        void passesAnOlderVersionThatIsDeprecated() {
            RuleFixture result = paths(rule, """
                    /v1/orders:
                      get: {deprecated: true, responses: {'200': {description: OK}}}
                    /v2/orders:
                      get: {responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsAnOlderVersionThatIsStillCurrent() {
            RuleFixture result = paths(rule, """
                    /v1/orders:
                      get: {responses: {'200': {description: OK}}}
                    /v2/orders:
                      get: {responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1v1~1orders/get");
        }

        @Test
        void doesNotApplyToAnApiWithOneVersionLeft() {
            RuleFixture result = paths(rule, """
                    /v2/orders:
                      get: {responses: {'200': {description: OK}}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }

    @Nested
    class DeprecatedOperationIsExplainedTest {

        private final DeprecatedOperationIsExplained rule = new DeprecatedOperationIsExplained();

        @Test
        void passesADeprecatedOperationThatNamesItsReplacement() {
            RuleFixture result = paths(rule, """
                    /v1/orders:
                      get: {deprecated: true, description: Replaced by GET /v2/orders., responses: {'200': {description: OK}}}
                    /v2/orders:
                      get: {responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsADeprecatedOperationWithOnlyASummary() {
            RuleFixture result = paths(rule, """
                    /v1/orders:
                      get: {deprecated: true, summary: List orders, description: ' ', responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1v1~1orders/get");
        }
    }

    @Nested
    class SunsetIsDateTest {

        private final SunsetIsDate rule = new SunsetIsDate();

        @Test
        void passesADateAndADateTime() {
            RuleFixture result = paths(rule, """
                    /v1/orders:
                      get: {x-sunset: '2027-06-30', responses: {'200': {description: OK}}}
                      post: {x-sunset: '2027-06-30T00:00:00Z', responses: {'201': {description: Created}}}
                    /v2/orders:
                      get: {responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsASunsetThatIsNotADate() {
            RuleFixture result = paths(rule, """
                    /v1/orders:
                      get: {x-sunset: next summer, responses: {'200': {description: OK}}}
                    """);

            assertThat(result.messages()).containsExactly("Sunset next summer is not a date.");
        }
    }
}
