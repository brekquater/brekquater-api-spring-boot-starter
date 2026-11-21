// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SchemaRulesTest {

    @Nested
    class PatternIsAnchoredTest {

        private final PatternIsAnchored rule = new PatternIsAnchored();

        @Test
        void passesAnAnchoredPattern() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      schemas:
                        Order:
                          type: object
                          properties:
                            status: {type: string, pattern: '^[A-Z]+$'}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsUnanchoredPatternsInPropertiesItemsAndParameters() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          parameters:
                            - {name: status, in: query, schema: {type: string, pattern: '[A-Z]+'}}
                          responses: {'200': {description: OK}}
                    components:
                      schemas:
                        Order:
                          type: object
                          properties:
                            tags: {type: array, items: {type: string, pattern: '^[a-z]+'}}
                    """);

            assertThat(result.failingLocations()).containsExactly(
                    "/components/schemas/Order/properties/tags/items",
                    "/paths/~1orders/get/parameters/0/schema");
        }

        @Test
        void treatsAnEscapedDollarAsALiteralCharacterAndAnEscapedBackslashAsNoEscape() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      schemas:
                        Price: {type: string, pattern: '^[0-9]+\\$'}
                        Path: {type: string, pattern: '^[a-z]+\\\\$'}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/schemas/Price");
        }

        @Test
        void doesNotApplyToSchemasWithoutAPattern() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      schemas:
                        Order: {type: object, properties: {id: {type: string}}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }

    @Nested
    class PropertyNamesShareOneCaseTest {

        private final PropertyNamesShareOneCase rule = new PropertyNamesShareOneCase();

        @Test
        void passesAnApiThatUsesOneStyle() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      schemas:
                        Order:
                          type: object
                          properties:
                            id: {type: string}
                            customer_name: {type: string}
                            placed_at: {type: string}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsTheNamesThatBreakTheMostCommonStyle() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      schemas:
                        LegacyError:
                          type: object
                          properties:
                            errorMessage: {type: string}
                            error_code: {type: string}
                            requestId: {type: string}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/schemas/LegacyError/properties/error_code");
            assertThat(result.messages()).containsExactly(
                    "Property error_code is snake_case, while most property names in this API are camelCase.");
        }

        @Test
        void prefersCamelCaseWhenTwoStylesAreEquallyCommon() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      schemas:
                        Order:
                          type: object
                          properties:
                            customerName: {type: string}
                            placed_at: {type: string}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/schemas/Order/properties/placed_at");
        }

        @Test
        void reportsANameThatMixesStylesEvenWhenItIsTheOnlyOne() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      schemas:
                        Order: {type: object, properties: {customer_Name: {type: string}}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/schemas/Order/properties/customer_Name");
        }

        @Test
        void ignoresSingleWordsAndNamesThatAreNotIdentifiers() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths: {}
                    components:
                      schemas:
                        Order:
                          type: object
                          properties:
                            id: {type: string}
                            status: {type: string}
                            '@type': {type: string}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }

        @Test
        void readsPropertiesOfInlineBodiesAndNestedObjects() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        post:
                          requestBody:
                            content:
                              application/json:
                                schema:
                                  type: object
                                  properties:
                                    customerName: {type: string}
                                    deliveryAddress:
                                      type: object
                                      properties:
                                        postal_code: {type: string}
                          responses: {'201': {description: Created}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1orders/post/requestBody/content/"
                    + "application~1json/schema/properties/deliveryAddress/properties/postal_code");
        }
    }
}
