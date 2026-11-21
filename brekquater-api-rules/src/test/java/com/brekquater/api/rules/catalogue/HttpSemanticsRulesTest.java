// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HttpSemanticsRulesTest {

    @Nested
    class OkResponseHasBodyTest {

        private final OkResponseHasBody rule = new OkResponseHasBody();

        @Test
        void passesA200WithABody() {
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
        void reportsA200WithoutABodyAsSpringdocDocumentsAVoidMethod() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders/{id}/archive:
                        post:
                          responses: {'200': {description: OK}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1orders~1{id}~1archive/post/responses/200");
        }

        @Test
        void acceptsAnEmpty200OnHeadAndOptions() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        head: {responses: {'200': {description: OK}}}
                        options: {responses: {'200': {description: OK}}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }

        @Test
        void leavesOtherSuccessCodesAlone() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders/{id}:
                        delete: {responses: {'204': {description: No Content}}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }

    @Nested
    class NoContentResponseHasNoBodyTest {

        private final NoContentResponseHasNoBody rule = new NoContentResponseHasNoBody();

        @Test
        void passesA204WithoutABody() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders/{id}:
                        delete: {responses: {'204': {description: No Content}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(1);
        }

        @Test
        void reportsEveryNoContentCodeThatDocumentsABody() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders/{id}:
                        get:
                          responses:
                            '304': {description: Not Modified, content: {application/json: {}}}
                        put:
                          responses:
                            '205': {description: Reset Content, content: {application/json: {}}}
                        delete:
                          responses:
                            '204': {description: No Content, content: {'*/*': {schema: {$ref: '#/components/schemas/Order'}}}}
                    """);

            assertThat(result.failingLocations()).containsExactly(
                    "/paths/~1orders~1{id}/get/responses/304",
                    "/paths/~1orders~1{id}/put/responses/205",
                    "/paths/~1orders~1{id}/delete/responses/204");
        }

        @Test
        void doesNotApplyToA200() {
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
    }

    @Nested
    class MediaTypeIsSpecificTest {

        private final MediaTypeIsSpecific rule = new MediaTypeIsSpecific();

        @Test
        void passesNamedMediaTypes() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        post:
                          requestBody: {content: {application/json: {}}}
                          responses:
                            '201': {description: Created, content: {application/json: {}}}
                            '400': {description: Bad Request, content: {application/problem+json: {}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(3);
        }

        @Test
        void reportsTheWildcardSpringdocWritesByDefaultAndAWildcardSubtype() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders:
                        post:
                          requestBody: {content: {'application/*': {}}}
                          responses:
                            '200': {description: OK, content: {'*/*': {}}}
                    """);

            assertThat(result.failingLocations()).containsExactly(
                    "/paths/~1orders/post/requestBody/content/application~1*",
                    "/paths/~1orders/post/responses/200/content/*~1*");
        }

        @Test
        void doesNotApplyToAnOperationWithoutBodies() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders/{id}:
                        delete: {responses: {'204': {description: No Content}}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }

    @Nested
    class PatchUsesPatchMediaTypeTest {

        private final PatchUsesPatchMediaType rule = new PatchUsesPatchMediaType();

        @Test
        void passesJsonMergePatchAndJsonPatch() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders/{id}:
                        patch:
                          requestBody:
                            content:
                              application/merge-patch+json: {}
                              application/json-patch+json; charset=utf-8: {}
                          responses: {'200': {description: OK}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(2);
        }

        @Test
        void reportsAPatchSentAsPlainJson() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders/{id}:
                        patch:
                          requestBody: {content: {application/json: {}}}
                          responses: {'200': {description: OK}}
                    """);

            assertThat(result.failingLocations())
                    .containsExactly("/paths/~1orders~1{id}/patch/requestBody/content/application~1json");
        }

        @Test
        void leavesOtherMethodsAlone() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders/{id}:
                        put:
                          requestBody: {content: {application/json: {}}}
                          responses: {'200': {description: OK}}
                    """);

            assertThat(result.applicableTargets()).isZero();
        }
    }

    @Nested
    class PathTemplatesAreDistinctTest {

        private final PathTemplatesAreDistinct rule = new PathTemplatesAreDistinct();

        @Test
        void passesPathsThatMatchDifferentRequests() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders/{id}: {get: {responses: {'200': {description: OK}}}}
                      /orders/{id}/items: {get: {responses: {'200': {description: OK}}}}
                      /orders/search: {get: {responses: {'200': {description: OK}}}}
                    """);

            assertThat(result.failingLocations()).isEmpty();
            assertThat(result.applicableTargets()).isEqualTo(3);
        }

        @Test
        void reportsBothPathsWhenOnlyTheVariableNamesDiffer() {
            RuleFixture result = RuleFixture.evaluate(rule, """
                    openapi: 3.1.0
                    info: {title: Orders API, version: "1.0"}
                    paths:
                      /orders/{id}: {get: {responses: {'200': {description: OK}}}}
                      /orders/{orderId}: {put: {responses: {'200': {description: OK}}}}
                    """);

            assertThat(result.failingLocations()).containsExactly("/paths/~1orders~1{id}", "/paths/~1orders~1{orderId}");
            assertThat(result.messages()).first().asString().contains("/orders/{orderId}");
        }
    }
}
