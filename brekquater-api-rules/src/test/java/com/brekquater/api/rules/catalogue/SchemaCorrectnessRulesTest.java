// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.OpenApiRule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SchemaCorrectnessRulesTest {

    private static RuleFixture schemas(OpenApiRule<?> rule, String namedSchemas) {
        return RuleFixture.evaluate(rule, """
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths: {}
                components:
                  schemas:
                """ + namedSchemas.indent(4));
    }

    @Nested
    class FormatFitsTypeTest {

        private final FormatFitsType rule = new FormatFitsType();

        @Test
        void passesMatchingFormatsAndNumbersWrittenAsStrings() {
            RuleFixture result = schemas(rule, """
                    Order: {type: object, properties: {at: {type: string, format: date-time}, count: {type: integer, format: int32}, big: {type: string, format: int64}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(3);
        }

        @Test
        void reportsADateTimeInteger() {
            RuleFixture result = schemas(rule, """
                    Order: {type: object, properties: {at: {type: integer, format: date-time}}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/schemas/Order/properties/at");
        }

        @Test
        void ignoresACustomFormatAndAFormatWithoutAType() {
            RuleFixture result = schemas(rule, """
                    Order: {type: object, properties: {iban: {type: integer, format: iban}, at: {format: date-time}}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }

    @Nested
    class EnumValuesMatchTypeTest {

        private final EnumValuesMatchType rule = new EnumValuesMatchType();

        @Test
        void passesWholeNumbersInANumberEnumAndANullInANullableOne() {
            RuleFixture result = schemas(rule, """
                    Ratio: {type: number, enum: [1, 1.5]}
                    Tier: {type: [string, 'null'], enum: [GOLD, null]}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsStringsInAnIntegerEnum() {
            RuleFixture result = schemas(rule, """
                    Size: {type: integer, enum: ['1', '2']}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/schemas/Size");
            assertThat(result.messages()).containsExactly("Enum values \"1\", \"2\" are not of the type integer.");
        }
    }

    @Nested
    class EnumValuesAreUniqueTest {

        private final EnumValuesAreUnique rule = new EnumValuesAreUnique();

        @Test
        void passesValuesThatDifferOnlyInCase() {
            assertThat(schemas(rule, "Tier: {type: string, enum: [gold, GOLD]}").failingLocations()).isEmpty();
        }

        @Test
        void reportsARepeatedValue() {
            RuleFixture result = schemas(rule, "Tier: {type: string, enum: [gold, silver, gold]}");

            assertThat(result.messages()).containsExactly("Enum lists these values more than once: \"gold\".");
        }
    }

    @Nested
    class DiscriminatorIsRequiredPropertyTest {

        private final DiscriminatorIsRequiredProperty rule = new DiscriminatorIsRequiredProperty();

        @Test
        void passesAPropertyEveryOneOfBranchRequires() {
            RuleFixture result = schemas(rule, """
                    Shape: {oneOf: [{$ref: '#/components/schemas/Circle'}, {$ref: '#/components/schemas/Square'}], discriminator: {propertyName: kind}}
                    Circle: {type: object, properties: {kind: {type: string}}, required: [kind]}
                    Square: {type: object, properties: {kind: {type: string}}, required: [kind]}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsAPropertyOneBranchDoesNotRequire() {
            RuleFixture result = schemas(rule, """
                    Shape: {oneOf: [{$ref: '#/components/schemas/Circle'}, {$ref: '#/components/schemas/Square'}], discriminator: {propertyName: kind}}
                    Circle: {type: object, properties: {kind: {type: string}}, required: [kind]}
                    Square: {type: object, properties: {kind: {type: string}}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/schemas/Shape");
        }

        @Test
        void reportsAPropertyNothingDefines() {
            RuleFixture result = schemas(rule, """
                    Shape: {type: object, required: [kind], discriminator: {propertyName: kind}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/schemas/Shape");
        }
    }

    @Nested
    class PropertyIsTypedTest {

        private final PropertyIsTyped rule = new PropertyIsTyped();

        @Test
        void passesTypesRefsEnumsAndMaps() {
            RuleFixture result = schemas(rule, """
                    Order:
                      type: object
                      properties:
                        id: {type: string}
                        self: {$ref: '#/components/schemas/Order'}
                        tier: {enum: [gold]}
                        tags: {additionalProperties: {}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(4);
        }

        @Test
        void doesNotMistakeTheKeywordsOfAPropertyNamedPropertiesForProperties() {
            RuleFixture result = schemas(rule, """
                    Problem: {type: object, properties: {properties: {type: object, additionalProperties: {}}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsAPropertyWithOnlyADescription() {
            RuleFixture result = schemas(rule, """
                    Order: {type: object, properties: {payload: {description: Anything}}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/schemas/Order/properties/payload");
        }
    }

    @Nested
    class PropertyNamesDifferBeyondCaseTest {

        private final PropertyNamesDifferBeyondCase rule = new PropertyNamesDifferBeyondCase();

        @Test
        void passesTheSameNameInDifferentObjects() {
            RuleFixture result = schemas(rule, """
                    Order: {type: object, properties: {url: {type: string}}}
                    Link: {type: object, properties: {URL: {type: string}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void reportsBothNamesThatDifferOnlyInCase() {
            RuleFixture result = schemas(rule, """
                    Order: {type: object, properties: {url: {type: string}, URL: {type: string}}}
                    """);

            assertThat(result.failingLocations())
                    .containsExactly("/components/schemas/Order/properties/URL", "/components/schemas/Order/properties/url");
        }
    }
}
