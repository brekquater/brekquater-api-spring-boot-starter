// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.Rule;
import com.brekquater.api.rules.RuleResult;
import com.brekquater.api.rules.openapi.OpenApiFixture;
import org.junit.jupiter.api.Test;

class OperationHasSummaryOrDescriptionTest {

    private final Rule rule = new OperationHasSummaryOrDescription();

    @Test
    void passesAnOperationWithASummary() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      summary: List orders
                      responses: {'200': {description: OK}}
                """));

        assertThat(result.findings()).isEmpty();
        assertThat(result.applicableTargets()).isEqualTo(1);
    }

    @Test
    void passesAnOperationWithOnlyADescription() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      description: Returns every order, newest first.
                      responses: {'200': {description: OK}}
                """));

        assertThat(result.findings()).isEmpty();
    }

    @Test
    void reportsAnOperationWithNeitherSummaryNorDescription() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders/{id}:
                    delete:
                      operationId: deleteOrder
                      responses: {'204': {description: No Content}}
                """));

        assertThat(result.findings())
                .singleElement()
                .satisfies(finding -> {
                    assertThat(finding.ruleId()).isEqualTo("BQ-DOC-001");
                    assertThat(finding.evidence().location()).isEqualTo("/paths/~1orders~1{id}/delete");
                    assertThat(finding.message()).isEqualTo("Operation has no summary or description.");
                    assertThat(finding.docUrl()).isEqualTo("https://starter.brekquater.com/api/rules/BQ-DOC-001");
                });
    }

    @Test
    void treatsWhitespaceOnlyTextAsMissing() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      summary: "   "
                      description: ""
                      responses: {'200': {description: OK}}
                """));

        assertThat(result.findings()).hasSize(1);
    }

    @Test
    void reportsOnlyTheUndocumentedOperationsOfAnApi() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      summary: List orders
                      responses: {'200': {description: OK}}
                    post:
                      responses: {'201': {description: Created}}
                  /customers:
                    get:
                      responses: {'200': {description: OK}}
                """));

        assertThat(result.applicableTargets()).isEqualTo(3);
        assertThat(result.passingTargets()).isEqualTo(1);
        assertThat(result.findings())
                .extracting(finding -> finding.evidence().location())
                .containsExactly("/paths/~1customers/get", "/paths/~1orders/post");
    }
}
