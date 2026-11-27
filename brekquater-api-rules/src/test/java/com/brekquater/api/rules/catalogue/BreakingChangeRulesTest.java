// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.ApiChanges;
import com.brekquater.api.rules.AssessmentContext;
import com.brekquater.api.rules.BuildReport;
import com.brekquater.api.rules.BuildReports;
import com.brekquater.api.rules.ReportFile;
import com.brekquater.api.rules.Rule;
import com.brekquater.api.rules.RuleResult;
import com.brekquater.api.rules.openapi.OpenApiFixture;
import com.brekquater.api.rules.openapi.SwaggerApiChanges;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BreakingChangeRulesTest {

    private static final String HEADER = """
            openapi: 3.1.0
            info: {title: Orders API, version: '%s'}
            paths:
            """;

    private static RuleFixture compare(Rule rule, String baselinePaths, String currentPaths) {
        return compare(rule, "1.0.0", baselinePaths, "1.1.0", currentPaths);
    }

    private static RuleFixture compare(Rule rule, String oldVersion, String oldPaths, String newVersion, String newPaths) {
        String baseline = document(oldVersion, oldPaths);
        String current = document(newVersion, newPaths);
        ApiChanges changes = SwaggerApiChanges.between(OpenApiFixture.parse(baseline), OpenApiFixture.parse(current));
        BuildReport<ApiChanges> report = BuildReport.read(changes, new ReportFile("openapi-baseline.json", Instant.EPOCH));
        AssessmentContext context = OpenApiFixture.context(current).withReports(BuildReports.none().withBaseline(report));
        RuleResult result = rule.evaluate(context);
        return new RuleFixture(
                result.applicableTargets(),
                result.findings().stream().map(finding -> finding.evidence().location()).toList(),
                result.findings().stream().map(finding -> finding.message()).toList());
    }

    // The paths are indented under "paths:"; a trailing "components:" block stays at the top level.
    private static String document(String version, String paths) {
        String[] parts = paths.split("(?m)^(?=components:)", 2);
        return HEADER.formatted(version) + parts[0].indent(2) + (parts.length > 1 ? parts[1] : "");
    }

    @Nested
    class RemovedOperationWasDeprecatedTest {

        private final RemovedOperationWasDeprecated rule = new RemovedOperationWasDeprecated();

        @Test
        void passesARenamedPathVariableAChangedOperationIdAndADeprecatedRemoval() {
            RuleFixture result = compare(rule, """
                    /orders/{id}:
                      get: {operationId: findOrder, responses: {'200': {description: OK}}}
                    /reports:
                      get: {deprecated: true, responses: {'200': {description: OK}}}
                    """, """
                    /orders/{orderId}:
                      get: {operationId: getOrder, responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsAnOperationRemovedWithoutWarning() {
            RuleFixture result = compare(rule, """
                    /orders:
                      get: {responses: {'200': {description: OK}}}
                      delete: {responses: {'204': {description: No Content}}}
                    """, """
                    /orders:
                      get: {responses: {'200': {description: OK}}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1orders/delete");
            assertThat(result.messages())
                    .containsExactly("DELETE /orders was removed, but the baseline did not mark it deprecated.");
        }
    }

    @Nested
    class ParameterDidNotBecomeRequiredTest {

        private final ParameterDidNotBecomeRequired rule = new ParameterDidNotBecomeRequired();

        @Test
        void passesNewOptionalParametersAndAHeaderWhoseNameOnlyChangedCase() {
            RuleFixture result = compare(rule, """
                    /orders/{id}:
                      get:
                        parameters: [{name: X-Tenant, in: header, required: true}]
                        responses: {'200': {description: OK}}
                    """, """
                    /orders/{orderId}:
                      get:
                        parameters:
                          - {name: orderId, in: path, required: true}
                          - {name: x-tenant, in: header, required: true}
                          - {name: expand, in: query}
                        responses: {'200': {description: OK}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void reportsAParameterThatBecameRequiredAndANewRequiredOne() {
            RuleFixture result = compare(rule, """
                    /orders:
                      get:
                        parameters: [{name: size, in: query}]
                        responses: {'200': {description: OK}}
                    """, """
                    /orders:
                      get:
                        parameters: [{name: size, in: query, required: true}, {name: region, in: query, required: true}]
                        responses: {'200': {description: OK}}
                    """);

            assertThat(result.failingLocations())
                    .containsExactly("/paths/~1orders/get/parameters/0", "/paths/~1orders/get/parameters/1");
            assertThat(result.messages()).first()
                    .isEqualTo("The query parameter size is required now, but was optional or absent in the baseline.");
        }
    }

    @Nested
    class RequestBodyDidNotBecomeRequiredTest {

        private final RequestBodyDidNotBecomeRequired rule = new RequestBodyDidNotBecomeRequired();

        private static final String OLD = """
                /orders:
                  post:
                    requestBody: {content: {application/json: {schema: {$ref: '#/components/schemas/NewOrder'}}}}
                    responses: {'201': {description: Created}}
                components:
                  schemas:
                    NewOrder: {type: object, properties: {item: {type: string}}, required: [item]}
                """;

        @Test
        void passesARequiredReadOnlyPropertyAndAnUnchangedBody() {
            RuleFixture result = compare(rule, OLD, """
                    /orders:
                      post:
                        requestBody: {content: {application/json: {schema: {$ref: '#/components/schemas/NewOrder'}}}}
                        responses: {'201': {description: Created}}
                    components:
                      schemas:
                        NewOrder:
                          type: object
                          properties: {item: {type: string}, id: {type: string, readOnly: true}, note: {type: string}}
                          required: [item, id]
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void reportsABodyThatBecameRequiredAndANewRequiredPropertyInAnAllOfBranch() {
            RuleFixture result = compare(rule, OLD, """
                    /orders:
                      post:
                        requestBody:
                          required: true
                          content: {application/json: {schema: {$ref: '#/components/schemas/NewOrder'}}}
                        responses: {'201': {description: Created}}
                    components:
                      schemas:
                        NewOrder:
                          allOf:
                            - {type: object, properties: {item: {type: string}}, required: [item]}
                            - {type: object, properties: {region: {type: string}}, required: [region]}
                    """);

            assertThat(result.failingLocations()).containsExactly(
                    "/components/schemas/NewOrder/allOf/1/properties/region", "/paths/~1orders/post/requestBody");
        }
    }

    @Nested
    class ResponsePropertyWasNotRemovedTest {

        private final ResponsePropertyWasNotRemoved rule = new ResponsePropertyWasNotRemoved();

        @Test
        void passesARemovedOptionalOrWriteOnlyProperty() {
            RuleFixture result = compare(rule, """
                    /orders/{id}:
                      get:
                        responses:
                          '200':
                            description: OK
                            content:
                              application/json:
                                schema:
                                  type: object
                                  properties: {id: {type: string}, note: {type: string}, secret: {type: string, writeOnly: true}}
                                  required: [id, secret]
                    """, """
                    /orders/{id}:
                      get:
                        responses:
                          '200':
                            description: OK
                            content: {application/json: {schema: {type: object, properties: {id: {type: string}}, required: [id]}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void comparesTheWildcardBodySpringdocWritesByDefault() {
            RuleFixture result = compare(rule, """
                    /orders/{id}:
                      get:
                        responses: {'200': {description: OK, content: {'*/*': {schema: {type: object, properties: {id: {type: string}}, required: [id]}}}}}
                    """, """
                    /orders/{id}:
                      get:
                        responses: {'200': {description: OK, content: {'*/*': {schema: {type: object, properties: {code: {type: string}}}}}}}
                    """);

            assertThat(result.failingLocations())
                    .containsExactly("/paths/~1orders~1{id}/get/responses/200/content/*~1*/schema");
        }

        @Test
        void reportsARequiredPropertyRemovedFromANestedSharedSchemaOnce() {
            RuleFixture result = compare(rule, """
                    /orders:
                      get:
                        responses: {'200': {description: OK, content: {application/json: {schema: {type: array, items: {$ref: '#/components/schemas/Order'}}}}}}
                    /orders/{id}:
                      get:
                        responses: {'200': {description: OK, content: {application/json: {schema: {$ref: '#/components/schemas/Order'}}}}}
                    components:
                      schemas:
                        Order: {type: object, properties: {id: {type: string}, total: {type: number}}, required: [id, total]}
                    """, """
                    /orders:
                      get:
                        responses: {'200': {description: OK, content: {application/json: {schema: {type: array, items: {$ref: '#/components/schemas/Order'}}}}}}
                    /orders/{id}:
                      get:
                        responses: {'200': {description: OK, content: {application/json: {schema: {$ref: '#/components/schemas/Order'}}}}}
                    components:
                      schemas:
                        Order: {type: object, properties: {id: {type: string}}, required: [id]}
                    """);

            assertThat(result.failingLocations()).containsExactly("/components/schemas/Order");
            assertThat(result.messages())
                    .containsExactly("Responses no longer have the property total, which the baseline marked required.");
        }
    }

    @Nested
    class RequestValueWasNotRemovedTest {

        private final RequestValueWasNotRemoved rule = new RequestValueWasNotRemoved();

        @Test
        void passesAnEnumThatGainedAValueOrWasDropped() {
            RuleFixture result = compare(rule, """
                    /orders:
                      get:
                        parameters: [{name: status, in: query, schema: {type: string, enum: [OPEN, CLOSED]}}, {name: sort, in: query, schema: {type: string, enum: [date]}}]
                        responses: {'200': {description: OK}}
                    """, """
                    /orders:
                      get:
                        parameters: [{name: status, in: query, schema: {type: string, enum: [OPEN, CLOSED, HELD]}}, {name: sort, in: query, schema: {type: string}}]
                        responses: {'200': {description: OK}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void reportsAValueClientsCanNoLongerSend() {
            RuleFixture result = compare(rule, """
                    /orders:
                      get:
                        parameters: [{name: status, in: query, schema: {type: string, enum: [OPEN, CLOSED]}}]
                        responses: {'200': {description: OK}}
                    """, """
                    /orders:
                      get:
                        parameters: [{name: status, in: query, schema: {type: string, enum: [OPEN]}}]
                        responses: {'200': {description: OK}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1orders/get/parameters/0/schema");
            assertThat(result.messages()).containsExactly("The query parameter status no longer accepts \"CLOSED\".");
        }
    }

    @Nested
    class SuccessStatusWasNotRemovedTest {

        private final SuccessStatusWasNotRemoved rule = new SuccessStatusWasNotRemoved();

        @Test
        void passesAStatusCoveredByARange() {
            RuleFixture result = compare(rule, """
                    /orders:
                      post: {responses: {'201': {description: Created}}}
                    """, """
                    /orders:
                      post: {responses: {'2XX': {description: Success}, '400': {description: Bad Request}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void reportsACreatedThatBecameAnOk() {
            RuleFixture result = compare(rule, """
                    /orders:
                      post: {responses: {'201': {description: Created}}}
                    """, """
                    /orders:
                      post: {responses: {'200': {description: OK}}}
                    """);

            assertThat(result.messages()).containsExactly("Operation no longer documents status 201, which the baseline did.");
        }
    }

    @Nested
    class TypeDidNotChangeTest {

        private final TypeDidNotChange rule = new TypeDidNotChange();

        @Test
        void passesIntegerAndNumberReplacingEachOtherAndANewlyNullableField() {
            RuleFixture result = compare(rule, """
                    /orders:
                      get:
                        parameters: [{name: size, in: query, schema: {type: integer}}]
                        responses: {'200': {description: OK, content: {application/json: {schema: {type: object, properties: {total: {type: number}}}}}}}
                    """, """
                    /orders:
                      get:
                        parameters: [{name: size, in: query, schema: {type: number}}]
                        responses: {'200': {description: OK, content: {application/json: {schema: {type: object, properties: {total: {type: [number, 'null']}}}}}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void reportsAPropertyThatTurnedFromAStringIntoAnArray() {
            RuleFixture result = compare(rule, """
                    /orders:
                      get:
                        responses: {'200': {description: OK, content: {application/json: {schema: {type: object, properties: {tag: {type: string}}}}}}}
                    """, """
                    /orders:
                      get:
                        responses: {'200': {description: OK, content: {application/json: {schema: {type: object, properties: {tag: {type: array, items: {type: string}}}}}}}}
                    """);

            assertThat(result.failingLocations()).containsExactly(
                    "/paths/~1orders/get/responses/200/content/application~1json/schema/properties/tag");
            assertThat(result.messages()).containsExactly("The property tag changed type from string to array.");
        }

        @Test
        void endsOnSchemasThatReferToThemselves() {
            String tree = """
                    /nodes:
                      get:
                        responses: {'200': {description: OK, content: {application/json: {schema: {$ref: '#/components/schemas/Node'}}}}}
                    components:
                      schemas:
                        Node: {type: object, properties: {children: {type: array, items: {$ref: '#/components/schemas/Node'}}}}
                    """;

            assertThat(compare(rule, tree, tree).failingLocations()).isEmpty();
        }
    }

    @Nested
    class MajorVersionMarksBreakingChangeTest {

        private final MajorVersionMarksBreakingChange rule = new MajorVersionMarksBreakingChange();

        private static final String BEFORE = """
                /orders:
                  get: {responses: {'200': {description: OK}}}
                  delete: {responses: {'204': {description: No Content}}}
                """;
        private static final String AFTER = """
                /orders:
                  get: {responses: {'200': {description: OK}}}
                """;

        @Test
        void passesABreakingChangeUnderANewMajorVersion() {
            RuleFixture result = compare(rule, "1.4.0", BEFORE, "2.0.0", AFTER);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsABreakingChangeUnderTheSameMajorVersion() {
            RuleFixture result = compare(rule, "1.4.0", BEFORE, "1.5.0", AFTER);

            assertThat(result.failingLocations()).containsExactly("/info/version");
            assertThat(result.messages()).first().asString().startsWith("The API has breaking changes since 1.4.0");
        }

        @Test
        void doesNotApplyWithoutABreakingChangeOrAReadableVersion() {
            assertThat(compare(rule, "1.4.0", AFTER, "1.4.1", AFTER).applicableTargets()).isZero();
            assertThat(compare(rule, "latest", BEFORE, "1.5.0", AFTER).applicableTargets()).isZero();
        }
    }

    @Nested
    class SunsetDidNotMoveEarlierTest {

        private final SunsetDidNotMoveEarlier rule = new SunsetDidNotMoveEarlier();

        @Test
        void passesALaterSunsetAndIgnoresOneThatIsNotADate() {
            RuleFixture result = compare(rule, """
                    /v1/orders:
                      get: {deprecated: true, x-sunset: '2027-06-30', responses: {'200': {description: OK}}}
                      post: {deprecated: true, x-sunset: soon, responses: {'201': {description: Created}}}
                    """, """
                    /v1/orders:
                      get: {deprecated: true, x-sunset: '2027-12-31', responses: {'200': {description: OK}}}
                      post: {deprecated: true, x-sunset: '2026-01-01', responses: {'201': {description: Created}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
        }

        @Test
        void reportsASunsetThatMovedEarlier() {
            RuleFixture result = compare(rule, """
                    /v1/orders:
                      get: {deprecated: true, x-sunset: '2027-06-30', responses: {'200': {description: OK}}}
                    """, """
                    /v1/orders:
                      get: {deprecated: true, x-sunset: '2027-03-31T00:00:00Z', responses: {'200': {description: OK}}}
                    """);

            assertThat(result.messages()).containsExactly("The sunset moved earlier, from 2027-06-30 to 2027-03-31.");
        }
    }
}
