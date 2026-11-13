// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.DocumentSummary;
import com.brekquater.api.rules.Finding;
import com.brekquater.api.rules.RuleEngine;
import com.brekquater.api.rules.RuleRegistry;
import com.brekquater.api.rules.report.Assessment;
import com.brekquater.api.rules.scoring.ScoreCalculator;
import com.brekquater.api.rules.scoring.ScoringPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EngineAssessmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");

    private final EngineAssessmentService service = new EngineAssessmentService(
            new RuleEngine(RuleRegistry.fromServiceLoader()),
            ScoreCalculator.riskWeighted(ScoringPolicy.defaults()),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void assessesAnOpenApi31DocumentAsSpringdocWritesIt() {
        Assessment assessment = service.assess(Optional.of(OpenApiDocument.of("""
                {"openapi":"3.1.0","info":{"title":"Orders API","version":"1.0"},
                 "paths":{"/orders":{"get":{"responses":{"200":{"description":"OK",
                   "content":{"application/json":{"schema":{"type":"object"}}}}}}}}}
                """)));

        assertThat(assessment.result().document()).contains(new DocumentSummary("3.1.0", "Orders API", "1.0", 1));
        assertThat(assessment.result().findings()).extracting(Finding::ruleId).contains("BQ-API-005", "BQ-SEC-001");
        assertThat(assessment.generatedAt()).isEqualTo(NOW);
    }

    @Test
    void assessesAnOpenApi30Document() {
        Assessment assessment = service.assess(Optional.of(OpenApiDocument.of("""
                {"openapi":"3.0.1","info":{"title":"Orders API","version":"1.0"},
                 "paths":{"/orders":{"get":{"responses":{"200":{"description":"OK",
                   "content":{"application/json":{"schema":{"type":"object"}}}}}}}}}
                """)));

        assertThat(assessment.result().findings()).extracting(Finding::ruleId).contains("BQ-API-005");
    }

    @Test
    void treatsAnUnreadableDocumentAsAbsent() {
        Assessment assessment = service.assess(Optional.of(OpenApiDocument.of("{\"openapi\": [")));

        assertThat(assessment.result().document()).isEmpty();
        assertThat(assessment.result().skippedRules()).isNotEmpty();
    }

    @Test
    void reportsNoDocumentAsNotAssessed() {
        Assessment assessment = service.assess(Optional.empty());

        assertThat(assessment.result().document()).isEmpty();
        assertThat(assessment.score().overall()).isEmpty();
    }
}
