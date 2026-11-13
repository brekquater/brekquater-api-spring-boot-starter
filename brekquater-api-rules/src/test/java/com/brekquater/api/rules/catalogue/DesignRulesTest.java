// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DesignRulesTest {

    @Nested
    class CollectionGetIsPaginatedTest {

        private final CollectionGetIsPaginated rule = new CollectionGetIsPaginated();

        @ParameterizedTest
        @ValueSource(strings = {"page", "size", "limit", "offset", "cursor", "pageable", "page_size", "pageSize"})
        void passesACollectionWithAPagingQueryParameter(String name) {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          parameters: [{name: %s, in: query, schema: {type: integer}}]
                          responses:
                            '200': {description: OK, content: {application/json: {schema: {type: array}}}}
                    """.formatted(name));

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsACollectionWithoutPagingParameters() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          parameters: [{name: status, in: query, schema: {type: string}}]
                          responses:
                            '200': {description: OK, content: {application/json: {schema: {type: array}}}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1orders/get");
        }

        @Test
        void aPagingNameInTheHeaderDoesNotCount() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          parameters: [{name: page, in: header, schema: {type: integer}}]
                          responses:
                            '200': {description: OK, content: {application/json: {schema: {type: array}}}}
                    """);

            assertThat(result.failingLocations()).hasSize(1);
        }

        @Test
        void readsParametersTheWholePathDeclares() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        parameters: [{$ref: '#/components/parameters/Limit'}]
                        get:
                          responses:
                            '200': {description: OK, content: {application/json: {schema: {type: array}}}}
                    components:
                      parameters:
                        Limit: {name: limit, in: query, schema: {type: integer}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void doesNotApplyToASingleResourceOrAnObjectResponse() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders/{id}/lines:
                        post:
                          responses:
                            '200': {description: OK, content: {application/json: {schema: {type: array}}}}
                      /orders/{id}:
                        get:
                          responses:
                            '200': {description: OK, content: {application/json: {schema: {type: array}}}}
                      /orders/page:
                        get:
                          responses:
                            '200': {description: OK, content: {application/json: {schema: {type: object}}}}
                            '400': {description: Bad, content: {application/json: {schema: {type: array}}}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }

    @Nested
    class GetHasNoRequestBodyTest {

        private final GetHasNoRequestBody rule = new GetHasNoRequestBody();

        @Test
        void passesAGetWithoutABodyAndIgnoresOtherMethods() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get: {responses: {'200': {description: OK}}}
                        post:
                          requestBody: {content: {application/json: {schema: {type: object}}}}
                          responses: {'201': {description: Created}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsAGetWithABody() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders/search:
                        get:
                          requestBody: {content: {application/json: {schema: {type: object}}}}
                          responses: {'200': {description: OK}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1orders~1search/get");
        }
    }
}
