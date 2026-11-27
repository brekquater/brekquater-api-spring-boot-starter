// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.AssessmentContext;
import com.brekquater.api.rules.CoverageTargets;
import com.brekquater.api.rules.ReportStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AssessmentInputsTest {

    private static final OpenApiDocument CURRENT = OpenApiDocument.of("""
            {"openapi": "3.1.0", "info": {"title": "Orders API", "version": "1.1.0"},
             "paths": {"/orders": {"get": {"responses": {"200": {"description": "OK"}}}}}}
            """);

    @TempDir
    Path project;

    private AssessmentInputs inputsWithBaseline(String content) throws IOException {
        Path baseline = Files.writeString(project.resolve("openapi-baseline.json"), content);
        ReportLocations locations = new ReportLocations(Optional.empty(), List.of(), List.of(), Optional.of(baseline));
        return new AssessmentInputs(new BuildReportFiles(locations), CoverageTargets.DEFAULTS);
    }

    @Test
    void comparesTheCurrentDocumentWithTheBaseline() throws IOException {
        AssessmentInputs inputs = inputsWithBaseline("""
                {"openapi": "3.1.0", "info": {"title": "Orders API", "version": "1.0.0"},
                 "paths": {"/orders": {"get": {"responses": {"200": {"description": "OK"}}},
                                       "delete": {"responses": {"204": {"description": "No Content"}}}}}}
                """);

        AssessmentContext context = inputs.contextFor(CURRENT);

        assertThat(context.changes().orElseThrow().changes()).extracting(ApiChange::kind)
                .containsExactly(ApiChange.Kind.OPERATION_REMOVED);
    }

    @Test
    void skipsOnlyTheBaselineRulesWhenTheBaselineCannotBeRead() throws IOException {
        AssessmentInputs inputs = inputsWithBaseline("{\"orders\": []}");

        AssessmentContext context = inputs.contextFor(CURRENT);

        assertThat(context.reports().baseline().status()).isEqualTo(ReportStatus.UNREADABLE);
        assertThat(context.openApi()).isPresent();
    }

    @Test
    void comparesNothingWithoutACurrentDocument() throws IOException {
        AssessmentInputs inputs = inputsWithBaseline("{\"openapi\": \"3.1.0\", \"paths\": {}}");

        assertThat(inputs.contextWithoutDocument().changes()).isEmpty();
        assertThat(inputs.contextFor(OpenApiDocument.of("not json")).changes()).isEmpty();
    }
}
