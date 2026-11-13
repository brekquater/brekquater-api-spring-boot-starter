// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResponseRulesTest {

    @Nested
    class ResponseContentHasSchemaTest {

        private final ResponseContentHasSchema rule = new ResponseContentHasSchema();

        @Test
        void passesABodyWithASchema() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          responses:
                            '200': {description: OK, content: {application/json: {schema: {type: array}}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsABodyWithoutASchema() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          responses:
                            '200': {description: OK, content: {application/json: {}}}
                    """);

            assertThat(result.failingLocations())
                    .containsExactly("/paths/~1orders/get/responses/200/content/application~1json");
        }

        @Test
        void doesNotApplyToAResponseWithoutABody() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders/{id}:
                        delete:
                          responses: {'204': {description: No Content}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }

        @Test
        void followsAReferenceToASharedResponse() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          responses:
                            '404': {$ref: '#/components/responses/NotFound'}
                    components:
                      responses:
                        NotFound: {description: Not Found, content: {application/json: {}}}
                    """);

            assertThat(result.failingLocations())
                    .containsExactly("/paths/~1orders/get/responses/404/content/application~1json");
        }
    }

    @Nested
    class ResponseSchemaIsNotFreeFormTest {

        private final ResponseSchemaIsNotFreeForm rule = new ResponseSchemaIsNotFreeForm();

        @ParameterizedTest
        @ValueSource(strings = {
            "{$ref: '#/components/schemas/Order'}",
            "{type: object, properties: {id: {type: string}}}",
            "{type: object, additionalProperties: {type: integer}}",
            "{type: object, additionalProperties: {$ref: '#/components/schemas/Order'}}",
            "{oneOf: [{$ref: '#/components/schemas/Order'}]}",
            "{type: array, items: {type: object}}",
            "{type: string}"
        })
        void passesASchemaThatDescribesItsShape(String schema) {
            assertThat(failingLocationsFor(schema)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "{type: object}",
            "{}",
            "{type: object, additionalProperties: true}",
            "{type: object, additionalProperties: {}}"
        })
        void reportsASchemaThatAllowsAnyObject(String schema) {
            assertThat(failingLocationsFor(schema))
                    .containsExactly("/paths/~1orders/get/responses/200/content/application~1json/schema");
        }

        @Test
        void readsTheSingleTypeOfAnOpenApi30Schema() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.0.3
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          responses:
                            '200': {description: OK, content: {application/json: {schema: {type: object}}}}
                    """);

            assertThat(result.failingLocations()).hasSize(1);
        }

        @Test
        void doesNotApplyToABodyWithoutASchema() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          responses:
                            '200': {description: OK, content: {application/json: {}}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }

        private java.util.List<String> failingLocationsFor(String schema) {
            return RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          responses:
                            '200': {description: OK, content: {application/json: {schema: %s}}}
                    components:
                      schemas:
                        Order: {type: object, properties: {id: {type: string}}}
                    """.formatted(schema)).failingLocations();
        }
    }

    @Nested
    class ErrorResponseUsesProblemDetailsTest {

        private final ErrorResponseUsesProblemDetails rule = new ErrorResponseUsesProblemDetails();

        @ParameterizedTest
        @ValueSource(strings = {"application/problem+json", "application/problem+xml", "APPLICATION/PROBLEM+JSON"})
        void passesAnErrorSentAsProblemDetails(String mediaType) {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          responses:
                            '400': {description: Bad Request, content: {'%s': {schema: {type: object}}}}
                    """.formatted(mediaType));

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsAnErrorSentAsAnyMediaType() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          responses:
                            '200': {description: OK, content: {application/json: {schema: {type: array}}}}
                            '404': {description: Not Found, content: {'*/*': {schema: {type: object}}}}
                            default: {description: Error, content: {application/json: {schema: {type: object}}}}
                    """);

            assertThat(result.failingLocations())
                    .containsExactly("/paths/~1orders/get/responses/404", "/paths/~1orders/get/responses/default");
        }

        @Test
        void doesNotApplyToAnErrorWithoutABody() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          responses: {'404': {description: Not Found}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }
}
