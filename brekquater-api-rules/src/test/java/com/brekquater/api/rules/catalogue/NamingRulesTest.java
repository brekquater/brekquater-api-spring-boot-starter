// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NamingRulesTest {

    @Nested
    class PathSegmentsAreKebabCaseTest {

        private final PathSegmentsAreKebabCase rule = new PathSegmentsAreKebabCase();

        @ParameterizedTest
        @ValueSource(strings = {"/orders", "/order-items/{orderItemId}", "/v2/orders", "/", "/orders/{id}.json"})
        void passesLowercaseKebabCasePaths(String path) {
            assertThat(failingLocationsFor(path)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"/orderItems", "/order_items", "/Orders", "/orders/line--items", "/orders/-items"})
        void reportsPathsWithOtherNaming(String path) {
            assertThat(failingLocationsFor(path)).hasSize(1);
        }

        @Test
        void namesTheOffendingSegments() {
            RuleFixture result = RuleFixture.evaluate(rule, documentWith("/Orders/{id}/lineItems"));

            assertThat(result.messages()).containsExactly(
                    "Path segments [Orders, lineItems] are not lowercase kebab-case.");
        }

        private List<String> failingLocationsFor(String path) {
            return RuleFixture.evaluate(rule, documentWith(path)).failingLocations();
        }
    }

    @Nested
    class PathHasNoTrailingSlashTest {

        private final PathHasNoTrailingSlash rule = new PathHasNoTrailingSlash();

        @Test
        void passesPathsWithoutATrailingSlashAndTheRootPath() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /: {get: {responses: {'200': {description: OK}}}}
                      /orders: {get: {responses: {'200': {description: OK}}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsAPathWithATrailingSlash() {
            assertThat(RuleFixture.evaluate(rule, documentWith("/orders/")).failingLocations())
                    .containsExactly("/paths/~1orders~1");
        }
    }

    @Nested
    class PathHasNoCrudVerbTest {

        private final PathHasNoCrudVerb rule = new PathHasNoCrudVerb();

        @ParameterizedTest
        @ValueSource(strings = {
            "/orders/{id}", "/addresses", "/listings", "/updates-feed", "/orders/{id}/cancellation", "/getaway"
        })
        void passesPathsThatNameResources(String path) {
            assertThat(RuleFixture.evaluate(rule, documentWith(path)).failingLocations()).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "/getOrders", "/orders/create", "/orders/{id}/delete", "/delete-order", "/list_orders", "/Update"
        })
        void reportsPathsThatStartASegmentWithACrudVerb(String path) {
            assertThat(RuleFixture.evaluate(rule, documentWith(path)).failingLocations()).hasSize(1);
        }

        @Test
        void namesTheOffendingSegments() {
            RuleFixture result = RuleFixture.evaluate(rule, documentWith("/fetchAll/orders/{id}/remove"));

            assertThat(result.messages()).containsExactly(
                    "Path segments [fetchAll, remove] name an action instead of a resource.");
        }
    }

    private static String documentWith(String path) {
        return """
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  '%s': {get: {responses: {'200': {description: OK}}}}
                """.formatted(path);
    }
}
