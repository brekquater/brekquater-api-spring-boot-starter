// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.brekquater.api.rules.RuleEngine;
import com.brekquater.api.rules.RuleRegistry;
import com.brekquater.api.rules.report.ReportWriter;
import com.brekquater.api.rules.scoring.ScoreCalculator;
import com.brekquater.api.rules.scoring.ScoringPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AssessmentEndpointTest {

    private static final String BASE_PATH = "brekquater.api.panel.base-path";
    private static final String ORDERS_API = """
            {"openapi": "3.1.0", "info": {"title": "Orders API", "version": "1.0"},
             "paths": {"/orders": {"get": {"responses": {"200": {"description": "OK"}}}}}}
            """;

    private final AssessmentService engine = new EngineAssessmentService(new EngineAssessmentService.Collaborators(
            AssessmentInputs.DOCUMENT_ONLY,
            new RuleEngine(RuleRegistry.fromServiceLoader()),
            ScoreCalculator.riskWeighted(ScoringPolicy.defaults()),
            Clock.fixed(Instant.parse("2026-11-13T10:00:00Z"), ZoneOffset.UTC)));

    @Test
    void servesTheAssessmentOfTheDocumentAsJson() throws Exception {
        MockMvc mvc = endpointWith(AccessGuard.everyone(), request -> Optional.of(OpenApiDocument.of(ORDERS_API)));

        mvc.perform(get("/brekquater/api/assessment"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.schemaVersion").value(1))
                .andExpect(jsonPath("$.status").value("ASSESSED"))
                .andExpect(jsonPath("$.document.title").value("Orders API"))
                .andExpect(jsonPath("$.generatedAt").value("2026-11-13T10:00:00Z"))
                .andExpect(jsonPath("$.findings[?(@.ruleId == 'BQ-SEC-001')]").exists());
    }

    @Test
    void reportsThatNothingWasAssessedWhenThereIsNoDocument() throws Exception {
        MockMvc mvc = endpointWith(AccessGuard.everyone(), request -> Optional.empty());

        mvc.perform(get("/brekquater/api/assessment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_ASSESSED"))
                .andExpect(jsonPath("$.reason").value("NO_OPENAPI_DOCUMENT"));
    }

    @Test
    void reportsAnUnreadableDocumentAsNotAssessed() throws Exception {
        MockMvc mvc = endpointWith(AccessGuard.everyone(), request -> Optional.of(OpenApiDocument.of("{not json")));

        mvc.perform(get("/brekquater/api/assessment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_ASSESSED"));
    }

    @Test
    void refusesACallerTheGuardDoesNotAdmitWithoutSayingMore() throws Exception {
        MockMvc mvc = endpointWith(() -> false, request -> Optional.of(OpenApiDocument.of(ORDERS_API)));

        mvc.perform(get("/brekquater/api/assessment"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().string("{\"title\":\"Unauthorized\",\"status\":401}"));
    }

    @Test
    void hidesTheCauseOfAFailure() throws Exception {
        MockMvc mvc = endpointWith(AccessGuard.everyone(), request -> {
            throw new IllegalStateException("database password is hunter2");
        });

        mvc.perform(get("/brekquater/api/assessment"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("{\"title\":\"Internal Server Error\",\"status\":500}"));
    }

    @Test
    void servesUnderTheConfiguredBasePath() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(endpoint(AccessGuard.everyone(), request -> Optional.empty()))
                .addPlaceholderValue(BASE_PATH, "/internal/api-quality")
                .build();

        mvc.perform(get("/internal/api-quality/assessment")).andExpect(status().isOk());
        mvc.perform(get("/brekquater/api/assessment")).andExpect(status().isNotFound());
    }

    private MockMvc endpointWith(AccessGuard guard, OpenApiDocumentSource source) {
        return MockMvcBuilders.standaloneSetup(endpoint(guard, source)).build();
    }

    private AssessmentEndpoint endpoint(AccessGuard guard, OpenApiDocumentSource source) {
        return new AssessmentEndpoint(new AssessmentEndpoint.Collaborators(guard, source, engine, ReportWriter.json()));
    }
}
