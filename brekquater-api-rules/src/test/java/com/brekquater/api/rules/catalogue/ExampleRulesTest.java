// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.OpenApiRule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExampleRulesTest {

    private static RuleFixture schemas(OpenApiRule<?> rule, String namedSchemas) {
        return RuleFixture.evaluate(rule, """
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths: {}
                components:
                  schemas:
                """ + namedSchemas.indent(4));
    }

    private static RuleFixture response(OpenApiRule<?> rule, String openApiVersion, String mediaType) {
        return RuleFixture.evaluate(rule, """
                openapi: %s
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      responses:
                        '200':
                          description: OK
                          content:
                """.formatted(openApiVersion) + mediaType.indent(14) + """
                components:
                  schemas:
                    Order:
                      type: object
                      required: [id, password]
                      properties:
                        id: {type: integer}
                        note: {type: string, maxLength: 3}
                        password: {type: string, writeOnly: true}
                """);
    }

    @Nested
    class SchemaExampleMatchesSchemaTest {

        private final SchemaExampleMatchesSchema rule = new SchemaExampleMatchesSchema();

        @Test
        void passesExamplesAndDefaultsTheSchemaAccepts() {
            RuleFixture result = schemas(rule, """
                    Order:
                      type: object
                      properties:
                        count: {type: integer, minimum: 1, example: 3, default: 1}
                        tier: {type: string, enum: [GOLD, SILVER], default: GOLD}
                        code: {type: string, pattern: '^[A-Z]+$', example: ABC}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(4);
        }

        @Test
        void reportsAWrongTypeABrokenLimitAndADefaultOutsideTheEnum() {
            RuleFixture result = schemas(rule, """
                    Order:
                      type: object
                      properties:
                        count: {type: integer, example: many}
                        over: {type: integer, maximum: 10, example: 50}
                        tier: {type: string, enum: [GOLD], default: BRONZE}
                    """);

            assertThat(result.failingLocations()).containsExactly(
                    "/components/schemas/Order/properties/count/example",
                    "/components/schemas/Order/properties/over/example",
                    "/components/schemas/Order/properties/tier/default");
            assertThat(result.messages()).first().asString().startsWith("Example or default does not match its schema:");
        }

        @Test
        void doesNotEnforceFormats() {
            RuleFixture result = schemas(rule, """
                    Stamp: {type: string, format: date-time, example: '2026-01-01T10:00:00'}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void staysQuietWhenTheSchemaCannotBeChecked() {
            RuleFixture result = schemas(rule, """
                    Loop: {allOf: [{$ref: '#/components/schemas/Loop'}], example: {}}
                    Regex: {type: string, pattern: '[', example: abc}
                    Remote: {$ref: 'https://example.com/schemas/remote.json'}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }
    }

    @Nested
    class ContentExampleMatchesSchemaTest {

        private final ContentExampleMatchesSchema rule = new ContentExampleMatchesSchema();

        @Test
        void passesABodyThatMatchesAndLeavesOutAWriteOnlyRequiredField() {
            RuleFixture result = response(rule, "3.1.0", """
                    application/json: {schema: {$ref: '#/components/schemas/Order'}, example: {id: 7, note: ok}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsANamedExampleThatBreaksAReferencedSchema() {
            RuleFixture result = response(rule, "3.1.0", """
                    application/json: {schema: {$ref: '#/components/schemas/Order'}, examples: {long: {value: {id: 7, note: too long}}}}
                    """);

            assertThat(result.failingLocations())
                    .containsExactly("/paths/~1orders/get/responses/200/content/application~1json/examples/long");
        }

        @Test
        void acceptsAnExampleOfOneOverlappingOneOfBranch() {
            RuleFixture result = response(rule, "3.1.0", """
                    application/json:
                      schema: {oneOf: [{type: object, properties: {a: {type: string}}}, {type: object, properties: {b: {type: string}}}]}
                      example: {a: x}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void readsTheNullableKeywordOfOpenApi30() {
            RuleFixture result = response(rule, "3.0.3", """
                    application/json: {schema: {type: string, nullable: true}, example: null, examples: {none: {value: text}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void doesNotCheckBodiesThatAreNotJson() {
            RuleFixture result = response(rule, "3.1.0", """
                    text/csv: {schema: {type: object}, example: 'a,b'}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }

        @Test
        void readsAParameterExampleAsTextAndSkipsStyledLists() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        get:
                          parameters:
                            - {name: size, in: query, schema: {type: integer}, example: '20'}
                            - {name: ids, in: query, schema: {type: array, items: {type: integer}}, example: '1,2'}
                            - {name: page, in: query, schema: {type: integer}, example: first}
                          responses: {'204': {description: No Content}}
                    """);

            assertThat(result.applicableTargets()).isEqualTo(2);
            assertThat(result.failingLocations()).containsExactly("/paths/~1orders/get/parameters/2/example");
        }
    }
}
