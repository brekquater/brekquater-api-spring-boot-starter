// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.Rule;
import com.brekquater.api.rules.RuleResult;
import com.brekquater.api.rules.openapi.OpenApiFixture;
import org.junit.jupiter.api.Test;

class OperationDocumentsErrorResponseTest {

    private final Rule rule = new OperationDocumentsErrorResponse();

    @Test
    void passesAnOperationThatDocumentsAClientError() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders/{id}:
                    get:
                      responses:
                        '200': {description: OK}
                        '404': {description: Not Found}
                """));

        assertThat(result.findings()).isEmpty();
        assertThat(result.applicableTargets()).isEqualTo(1);
    }

    @Test
    void passesAnOperationThatDocumentsOnlyAServerError() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      responses:
                        '200': {description: OK}
                        '503': {description: Service Unavailable}
                """));

        assertThat(result.findings()).isEmpty();
    }

    @Test
    void passesAnOperationThatDocumentsAnErrorRange() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      responses:
                        '200': {description: OK}
                        4XX: {description: Client error}
                """));

        assertThat(result.findings()).isEmpty();
    }

    @Test
    void passesAnOperationThatDocumentsADefaultResponse() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      responses:
                        '200': {description: OK}
                        default: {description: Unexpected error}
                """));

        assertThat(result.findings()).isEmpty();
    }

    @Test
    void reportsAnOperationThatDocumentsOnlySuccess() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    post:
                      responses:
                        '201': {description: Created}
                        2XX: {description: Other success}
                        '302': {description: Found}
                """));

        assertThat(result.findings())
                .singleElement()
                .satisfies(finding -> {
                    assertThat(finding.ruleId()).isEqualTo("BQ-ERR-001");
                    assertThat(finding.evidence().location()).isEqualTo("/paths/~1orders/post");
                    assertThat(finding.message()).isEqualTo("Operation documents no error response.");
                });
    }

    @Test
    void reportsAnOperationWithNoResponsesAtAll() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get: {}
                """));

        assertThat(result.findings()).hasSize(1);
    }

    @Test
    void passesWhatSpringdocGeneratesWhenAControllerAdviceDeclaresErrorStatuses() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: OpenAPI definition, version: v0}
                servers:
                  - {url: 'http://localhost:8080', description: Generated server url}
                paths:
                  /orders/{id}:
                    get:
                      tags: [order-controller]
                      operationId: findOrder
                      parameters:
                        - {name: id, in: path, required: true, schema: {type: string}}
                      responses:
                        '400':
                          description: Bad Request
                          content: {'*/*': {schema: {$ref: '#/components/schemas/ProblemDetail'}}}
                        '404':
                          description: Not Found
                          content: {'*/*': {schema: {$ref: '#/components/schemas/ProblemDetail'}}}
                        '200':
                          description: OK
                          content: {'*/*': {schema: {$ref: '#/components/schemas/Order'}}}
                components:
                  schemas:
                    ProblemDetail: {type: object, properties: {title: {type: string}}}
                    Order: {type: object, properties: {id: {type: string}}}
                """));

        assertThat(result.findings()).isEmpty();
    }

    @Test
    void reportsWhatSpringdocGeneratesWhenNoErrorHandlingIsDeclared() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: OpenAPI definition, version: v0}
                servers:
                  - {url: 'http://localhost:8080', description: Generated server url}
                paths:
                  /orders/{id}:
                    get:
                      tags: [order-controller]
                      operationId: findOrder
                      parameters:
                        - {name: id, in: path, required: true, schema: {type: string}}
                      responses:
                        '200':
                          description: OK
                          content: {'*/*': {schema: {$ref: '#/components/schemas/Order'}}}
                components:
                  schemas:
                    Order: {type: object, properties: {id: {type: string}}}
                """));

        assertThat(result.findings())
                .extracting(finding -> finding.evidence().location())
                .containsExactly("/paths/~1orders~1{id}/get");
    }
}
