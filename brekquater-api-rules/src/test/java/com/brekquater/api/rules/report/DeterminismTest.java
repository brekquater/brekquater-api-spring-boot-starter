// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.AssessmentContext;
import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.RuleEngine;
import com.brekquater.api.rules.RuleRegistry;
import com.brekquater.api.rules.openapi.OpenApiFixture;
import com.brekquater.api.rules.openapi.SwaggerOpenApiContext;
import com.brekquater.api.rules.scoring.ScoreCalculator;
import com.brekquater.api.rules.scoring.ScoringPolicy;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** The same API must give the same report, byte for byte, however often it is assessed and however it is ordered. */
class DeterminismTest {

    private static final Instant GENERATED_AT = Instant.parse("2026-11-13T10:00:00Z");
    private static final int RUNS = 25;

    private static final String ORDERS_API = """
            openapi: 3.1.0
            info: {title: Orders API, version: "1.0"}
            paths:
              /orders:
                get:
                  responses: {'200': {description: OK}}
                post:
                  operationId: createOrder
                  responses: {'201': {description: Created}}
              /orders/{id}:
                get:
                  summary: Find an order
                  responses: {'200': {description: OK}, '404': {description: Not Found}}
                delete:
                  responses: {'204': {description: No Content}}
              /customers:
                get:
                  operationId: listCustomers
                  responses: {'200': {description: OK}}
              /customers/{id}/orders:
                get:
                  responses: {default: {description: Error}}
            """;

    private final RuleEngine engine = new RuleEngine(RuleRegistry.fromServiceLoader());

    @Test
    void repeatedAssessmentsGiveIdenticalReports() {
        List<String> reports = IntStream.range(0, RUNS)
                .mapToObj(run -> reportOf(OpenApiFixture.parse(ORDERS_API)))
                .toList();

        assertThat(reports).first().asString().contains("BQ-DOC-001");
        assertThat(reports).containsOnly(reports.get(0));
    }

    @Test
    void theOrderOfThePathsInTheDocumentDoesNotChangeTheReport() {
        String reportInWrittenOrder = reportOf(OpenApiFixture.parse(ORDERS_API));

        List<String> reportsInShuffledOrder = IntStream.range(0, RUNS)
                .mapToObj(seed -> reportOf(withShuffledPaths(OpenApiFixture.parse(ORDERS_API), seed)))
                .toList();

        assertThat(reportsInShuffledOrder).containsOnly(reportInWrittenOrder);
    }

    private static OpenAPI withShuffledPaths(OpenAPI openApi, int seed) {
        List<String> pathNames = new ArrayList<>(openApi.getPaths().keySet());
        Collections.shuffle(pathNames, new Random(seed));
        Paths shuffled = new Paths();
        pathNames.forEach(name -> shuffled.addPathItem(name, openApi.getPaths().get(name)));
        return openApi.paths(shuffled);
    }

    private String reportOf(OpenAPI openApi) {
        AssessmentResult result = engine.assess(AssessmentContext.of(new SwaggerOpenApiContext(openApi)));
        Assessment assessment = new Assessment(
                result, ScoreCalculator.riskWeighted(ScoringPolicy.defaults()).score(result), GENERATED_AT);
        StringWriter output = new StringWriter();
        ReportWriter.json().write(assessment, output);
        return output.toString();
    }
}
