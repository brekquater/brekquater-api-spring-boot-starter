// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.AssessmentContext;
import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.BuildReport;
import com.brekquater.api.rules.BuildReports;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Coverage;
import com.brekquater.api.rules.CoverageCounter;
import com.brekquater.api.rules.Evidence;
import com.brekquater.api.rules.FailedTest;
import com.brekquater.api.rules.Finding;
import com.brekquater.api.rules.ReportFile;
import com.brekquater.api.rules.RuleEngine;
import com.brekquater.api.rules.RuleInput;
import com.brekquater.api.rules.RuleOutcome;
import com.brekquater.api.rules.RuleRegistry;
import com.brekquater.api.rules.Severity;
import com.brekquater.api.rules.TestResults;
import com.brekquater.api.rules.catalogue.OperationDocumentsErrorResponse;
import com.brekquater.api.rules.catalogue.OperationHasSummaryOrDescription;
import com.brekquater.api.rules.openapi.OpenApiFixture;
import com.brekquater.api.rules.scoring.ScoreCalculator;
import com.brekquater.api.rules.scoring.ScoringPolicy;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class JsonReportWriterTest {

    private static final Instant GENERATED_AT = Instant.parse("2026-11-13T10:00:00.987Z");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final AssessmentContext ORDERS_API = OpenApiFixture.context("""
            openapi: 3.1.0
            info: {title: Orders API, version: "1.0"}
            paths:
              /orders:
                get:
                  summary: List orders
                  responses: {'200': {description: OK}}
                post:
                  responses:
                    '201': {description: Created}
                    '400': {description: Bad Request}
            """);

    private static final ReportFile REPORT_FILE = new ReportFile("jacoco.xml", Instant.parse("2026-11-13T09:58:00.5Z"));

    private static final BuildReports TESTED_BUILD = new BuildReports(
            BuildReport.read(
                    new Coverage(new CoverageCounter(22, 78), new CoverageCounter(35, 65), List.of()), REPORT_FILE),
            BuildReport.read(
                    new TestResults(10, 2, List.of(new FailedTest("com.example.OrdersTest", "listsOrders"))),
                    new ReportFile("surefire-reports", REPORT_FILE.modifiedAt())),
            BuildReport.unreadable(new ReportFile("osv-scanner.json", REPORT_FILE.modifiedAt())));

    private final RuleEngine engine = new RuleEngine(
            new RuleRegistry(List.of(new OperationHasSummaryOrDescription(), new OperationDocumentsErrorResponse())));

    @Test
    void writesTheAssessmentContract() {
        JsonNode report = JSON.readTree(write(ORDERS_API));

        assertThat(report).isEqualTo(JSON.readTree("""
                {
                  "schemaVersion": 1,
                  "generatedAt": "2026-11-13T10:00:00Z",
                  "ruleSetVersion": "development",
                  "status": "ASSESSED",
                  "document": {"openapi": "3.1.0", "title": "Orders API", "version": "1.0", "operations": 2},
                  "inputs": [
                    {"type": "OPENAPI", "status": "READ"},
                    {"type": "COVERAGE", "status": "MISSING", "hint": "Run the build with the jacoco-maven-plugin \
                report goal, for example ./mvnw verify. See https://starter.brekquater.com/api/reports"},
                    {"type": "TEST_RESULTS", "status": "MISSING", "hint": "Run the tests, for example ./mvnw verify, \
                to write Surefire reports. See https://starter.brekquater.com/api/reports"},
                    {"type": "VULNERABILITIES", "status": "MISSING", "hint": "Run OSV-Scanner or OWASP \
                Dependency-Check with JSON output. See https://starter.brekquater.com/api/reports"},
                    {"type": "BASELINE", "status": "MISSING", "hint": "Commit the OpenAPI document of your last \
                release as openapi-baseline.json. See https://starter.brekquater.com/api/reports"}
                  ],
                  "score": {"overall": 50, "grade": "D", "assessedCategories": 2, "totalCategories": 8},
                  "securityRating": null,
                  "blockers": 0,
                  "categories": [
                    {"name": "DESIGN", "assessed": false, "score": null, "reason": "NOT_IMPLEMENTED"},
                    {"name": "DOCUMENTATION", "assessed": true, "score": 50},
                    {"name": "SECURITY", "assessed": false, "score": null, "reason": "NOT_IMPLEMENTED"},
                    {"name": "ERRORS", "assessed": true, "score": 50},
                    {"name": "NAMING", "assessed": false, "score": null, "reason": "NOT_IMPLEMENTED"},
                    {"name": "TESTING", "assessed": false, "score": null, "reason": "NOT_IMPLEMENTED"},
                    {"name": "CODE_QUALITY", "assessed": false, "score": null, "reason": "NOT_IMPLEMENTED"},
                    {"name": "ARCHITECTURE", "assessed": false, "score": null, "reason": "NOT_IMPLEMENTED"}
                  ],
                  "findings": [
                    {
                      "ruleId": "BQ-ERR-001",
                      "severity": "MAJOR",
                      "category": "ERRORS",
                      "message": "Operation documents no error response.",
                      "evidence": {"source": "OPENAPI", "location": "/paths/~1orders/get"},
                      "remediation": "Add @ApiResponse(responseCode = \\"400\\", ...) to the controller method, or \
                handle the error in a @ControllerAdvice method annotated with @ResponseStatus.",
                      "docUrl": "https://starter.brekquater.com/api/rules/BQ-ERR-001"
                    },
                    {
                      "ruleId": "BQ-DOC-001",
                      "severity": "MINOR",
                      "category": "DOCUMENTATION",
                      "message": "Operation has no summary or description.",
                      "evidence": {"source": "OPENAPI", "location": "/paths/~1orders/post"},
                      "remediation": "Add @Operation(summary = \\"...\\") to the controller method.",
                      "docUrl": "https://starter.brekquater.com/api/rules/BQ-DOC-001"
                    }
                  ],
                  "failedRules": []
                }
                """));
    }

    @Test
    void writesTheMembersInTheDocumentedOrder() {
        JsonNode report = JSON.readTree(write(ORDERS_API));

        assertThat(report.propertyNames())
                .containsExactly(
                        "schemaVersion",
                        "generatedAt",
                        "ruleSetVersion",
                        "status",
                        "document",
                        "inputs",
                        "score",
                        "securityRating",
                        "blockers",
                        "categories",
                        "findings",
                        "failedRules");
    }

    @Test
    void saysWhyNothingWasAssessedWhenThereIsNoOpenApiDocument() {
        JsonNode report = JSON.readTree(write(AssessmentContext.empty()));

        assertThat(report.get("status").asString()).isEqualTo("NOT_ASSESSED");
        assertThat(report.get("reason").asString()).isEqualTo("NO_OPENAPI_DOCUMENT");
        assertThat(report.get("document").isNull()).isTrue();
        assertThat(report.get("inputs").get(0)).isEqualTo(JSON.readTree("""
                {"type": "OPENAPI", "status": "MISSING"}
                """));
        assertThat(report.get("score")).isEqualTo(JSON.readTree("""
                {"overall": null, "grade": null, "assessedCategories": 0, "totalCategories": 8}
                """));
        assertThat(report.get("categories").get(0)).isEqualTo(JSON.readTree("""
                {"name": "DESIGN", "assessed": false, "score": null, "reason": "NOT_IMPLEMENTED"}
                """));
        assertThat(report.get("categories").get(1)).isEqualTo(JSON.readTree("""
                {"name": "DOCUMENTATION", "assessed": false, "score": null, "reason": "MISSING_INPUT"}
                """));
    }

    @Test
    void namesEachBuildReportByFileNameAndSaysWhenItWasWritten() {
        JsonNode report = JSON.readTree(write(ORDERS_API.withReports(TESTED_BUILD)));

        assertThat(report.get("inputs").get(1)).isEqualTo(JSON.readTree("""
                {"type": "COVERAGE", "status": "READ", "file": "jacoco.xml", "modifiedAt": "2026-11-13T09:58:00Z"}
                """));
        assertThat(report.get("inputs").get(3)).isEqualTo(JSON.readTree("""
                {"type": "VULNERABILITIES", "status": "UNREADABLE", "file": "osv-scanner.json",
                 "modifiedAt": "2026-11-13T09:58:00Z", "hint": "The report was found but could not be read; the \
                application log says why. See https://starter.brekquater.com/api/reports"}
                """));
    }

    @Test
    void summarisesCoverageAndTestResultsInTheTestingCategory() {
        JsonNode report = JSON.readTree(write(ORDERS_API.withReports(TESTED_BUILD)));

        assertThat(report.get("categories").get(5).get("metrics")).isEqualTo(JSON.readTree("""
                {"lineCoverage": 78, "branchCoverage": 65, "testsPassed": 7, "testsFailed": 1, "testsSkipped": 2}
                """));
    }

    @Test
    void leavesOutBranchCoverageWhenTheProjectHasNoBranches() {
        Coverage noBranches = new Coverage(new CoverageCounter(1, 9), new CoverageCounter(0, 0), List.of());
        BuildReports reports = new BuildReports(
                BuildReport.read(noBranches, REPORT_FILE), BuildReport.missing(), BuildReport.missing());

        JsonNode report = JSON.readTree(write(ORDERS_API.withReports(reports)));

        assertThat(report.get("categories").get(5).get("metrics")).isEqualTo(JSON.readTree("""
                {"lineCoverage": 90}
                """));
    }

    @Test
    void countsBlockersAndRatesSecurityWhenASecurityRuleWasEvaluated() {
        Evidence evidence = new Evidence(RuleInput.OPENAPI, "/paths/~1orders/get");
        Finding blocker = new Finding(
                "BQ-TST-901", Severity.BLOCKER, Category.SECURITY, "Unprotected.", evidence, "Protect it.", "https://x");
        RuleOutcome outcome = new RuleOutcome("BQ-TST-901", Severity.BLOCKER, Category.SECURITY, 2, 1);
        AssessmentResult result = new AssessmentResult(
                "test", Optional.empty(), BuildReports.none(), List.of(blocker), List.of(outcome), List.of(), List.of());

        JsonNode report = JSON.readTree(write(result));

        assertThat(report.get("blockers").asInt()).isEqualTo(1);
        assertThat(report.get("securityRating").asString()).isEqualTo("E");
        assertThat(report.get("score").get("overall").asInt()).isEqualTo(50);
    }

    private String write(AssessmentContext context) {
        return write(engine.assess(context));
    }

    private static String write(AssessmentResult result) {
        Assessment assessment = new Assessment(
                result, ScoreCalculator.riskWeighted(ScoringPolicy.defaults()).score(result), GENERATED_AT);
        StringWriter output = new StringWriter();
        ReportWriter.json().write(assessment, output);
        return output.toString();
    }
}
