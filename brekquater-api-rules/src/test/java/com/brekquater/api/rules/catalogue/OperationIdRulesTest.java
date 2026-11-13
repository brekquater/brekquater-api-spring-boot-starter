// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OperationIdRulesTest {

    private static final String THREE_OPERATIONS = """
            openapi: 3.1.0
            info: {title: Orders API, version: "1.0"}
            paths:
              /orders:
                get: {operationId: listOrders, responses: {'200': {description: OK}}}
                post: {operationId: listOrders, responses: {'201': {description: Created}}}
              /customers:
                get: {responses: {'200': {description: OK}}}
            """;

    @Nested
    class OperationIdIsUniqueTest {

        private final OperationIdIsUnique rule = new OperationIdIsUnique();

        @Test
        void passesOperationIdsThatAreAllDifferent() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get: {operationId: listOrders, responses: {'200': {description: OK}}}
                        post: {operationId: createOrder, responses: {'201': {description: Created}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsEveryOperationThatSharesAnOperationId() {
            RuleFixture result = RuleFixture.evaluate(rule, THREE_OPERATIONS);

            assertThat(result.failingLocations()).containsExactly("/paths/~1orders/get", "/paths/~1orders/post");
            assertThat(result.messages()).first().asString().contains("\"listOrders\"");
        }

        @Test
        void doesNotCountOperationsWithoutAnOperationId() {
            assertThat(RuleFixture.evaluate(rule, THREE_OPERATIONS).applicableTargets()).isEqualTo(2);
        }
    }

    @Nested
    class OperationIdHasNoGeneratedSuffixTest {

        private final OperationIdHasNoGeneratedSuffix rule = new OperationIdHasNoGeneratedSuffix();

        @Test
        void passesAnOperationIdWithoutASuffix() {
            assertThat(RuleFixture.evaluate(rule, THREE_OPERATIONS).failingLocations()).isEmpty();
        }

        @Test
        void reportsAnOperationIdThatEndsInAnUnderscoreAndDigits() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get: {operationId: findAll, responses: {'200': {description: OK}}}
                      /customers:
                        get: {operationId: findAll_12, responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1customers/get");
        }

        @Test
        void passesDigitsThatAreNotAfterAnUnderscore() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get: {operationId: listOrdersV2, responses: {'200': {description: OK}}}
                      /customers:
                        get: {operationId: _1, responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }
    }
}
