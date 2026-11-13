// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.Rule;
import com.brekquater.api.rules.RuleResult;
import com.brekquater.api.rules.openapi.OpenApiFixture;
import org.junit.jupiter.api.Test;

class OperationHasOperationIdTest {

    private final Rule rule = new OperationHasOperationId();

    @Test
    void passesAnOperationWithAnOperationId() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      operationId: listOrders
                      responses: {'200': {description: OK}}
                """));

        assertThat(result.findings()).isEmpty();
        assertThat(result.applicableTargets()).isEqualTo(1);
    }

    @Test
    void reportsAnOperationWithoutAnOperationId() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      summary: List orders
                      responses: {'200': {description: OK}}
                """));

        assertThat(result.findings())
                .singleElement()
                .satisfies(finding -> {
                    assertThat(finding.ruleId()).isEqualTo("BQ-API-001");
                    assertThat(finding.evidence().location()).isEqualTo("/paths/~1orders/get");
                    assertThat(finding.message()).isEqualTo("Operation has no operationId.");
                });
    }

    @Test
    void treatsABlankOperationIdAsMissing() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      operationId: " "
                      responses: {'200': {description: OK}}
                """));

        assertThat(result.findings()).hasSize(1);
    }

    @Test
    void checksEveryOperationOfAPath() {
        RuleResult result = rule.evaluate(OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    get:
                      operationId: listOrders
                      responses: {'200': {description: OK}}
                    post:
                      responses: {'201': {description: Created}}
                """));

        assertThat(result.applicableTargets()).isEqualTo(2);
        assertThat(result.findings())
                .extracting(finding -> finding.evidence().location())
                .containsExactly("/paths/~1orders/post");
    }
}
