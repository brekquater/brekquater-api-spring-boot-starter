// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.report;

import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.BuildReport;
import com.brekquater.api.rules.BuildReports;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Coverage;
import com.brekquater.api.rules.DocumentSummary;
import com.brekquater.api.rules.FailedRule;
import com.brekquater.api.rules.Finding;
import com.brekquater.api.rules.ReportStatus;
import com.brekquater.api.rules.RuleInput;
import com.brekquater.api.rules.Severity;
import com.brekquater.api.rules.TestResults;
import com.brekquater.api.rules.scoring.CategoryScore;
import com.brekquater.api.rules.scoring.Score;
import com.brekquater.api.rules.scoring.SecurityRating;
import java.io.Writer;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Writes the assessment JSON contract. The contract is public and versioned by {@code schemaVersion}: a field is
 * never renamed or removed without raising that number.
 *
 * <p>Members are written in a fixed order, so two assessments of the same inputs differ only in
 * {@code generatedAt}.
 */
final class JsonReportWriter implements ReportWriter {

    private static final int SCHEMA_VERSION = 1;
    private static final String ASSESSED = "ASSESSED";
    private static final String NOT_ASSESSED = "NOT_ASSESSED";
    private static final String NO_OPENAPI_DOCUMENT = "NO_OPENAPI_DOCUMENT";
    private static final String MISSING_INPUT = "MISSING_INPUT";
    private static final String NOT_IMPLEMENTED = "NOT_IMPLEMENTED";

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public void write(Assessment assessment, Writer output) {
        mapper.writeValue(output, report(assessment));
    }

    private ObjectNode report(Assessment assessment) {
        AssessmentResult result = assessment.result();
        ObjectNode report = mapper.createObjectNode();
        report.put("schemaVersion", SCHEMA_VERSION);
        report.put("generatedAt", assessment.generatedAt().truncatedTo(ChronoUnit.SECONDS).toString());
        report.put("ruleSetVersion", result.ruleSetVersion());
        putStatus(report, result);
        report.set("score", score(assessment.score()));
        report.put("securityRating", SecurityRating.of(result).map(Enum::name).orElse(null));
        report.put("blockers", blockerCount(result));
        report.set("categories", categories(assessment));
        report.set("findings", findings(result));
        report.set("failedRules", failedRules(result));
        return report;
    }

    private void putStatus(ObjectNode report, AssessmentResult result) {
        boolean documentRead = result.document().isPresent();
        report.put("status", documentRead ? ASSESSED : NOT_ASSESSED);
        if (!documentRead) {
            report.put("reason", NO_OPENAPI_DOCUMENT);
        }
        report.set("document", result.document().map(this::document).orElse(null));
        report.set("inputs", inputs(documentRead, result.reports()));
    }

    private ArrayNode inputs(boolean documentRead, BuildReports reports) {
        ArrayNode inputs = mapper.createArrayNode();
        ObjectNode openApi = inputs.addObject();
        openApi.put("type", RuleInput.OPENAPI.name());
        openApi.put("status", (documentRead ? ReportStatus.READ : ReportStatus.MISSING).name());
        inputs.add(input(RuleInput.COVERAGE, reports.coverage()));
        inputs.add(input(RuleInput.TEST_RESULTS, reports.testResults()));
        inputs.add(input(RuleInput.VULNERABILITIES, reports.vulnerabilities()));
        inputs.add(documentRead ? input(RuleInput.BASELINE, reports.baseline()) : baselineNotCompared());
        return inputs;
    }

    // Without a current document there is nothing to compare, whether or not a baseline file exists.
    private ObjectNode baselineNotCompared() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", RuleInput.BASELINE.name());
        node.put("status", ReportStatus.MISSING.name());
        node.put("hint", ReportHints.BASELINE_NOT_COMPARED);
        return node;
    }

    private ObjectNode input(RuleInput type, BuildReport<?> report) {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", type.name());
        node.put("status", report.status().name());
        report.file().ifPresent(file -> {
            node.put("file", file.name());
            node.put("modifiedAt", file.modifiedAt().truncatedTo(ChronoUnit.SECONDS).toString());
        });
        if (report.status() != ReportStatus.READ) {
            node.put("hint", ReportHints.of(type, report.status()));
        }
        return node;
    }

    private ObjectNode document(DocumentSummary summary) {
        ObjectNode document = mapper.createObjectNode();
        document.put("openapi", summary.openApiVersion());
        document.put("title", summary.title());
        document.put("version", summary.version());
        document.put("operations", summary.operationCount());
        return document;
    }

    private ObjectNode score(Score score) {
        ObjectNode node = mapper.createObjectNode();
        score.overall().ifPresentOrElse(value -> node.put("overall", value), () -> node.putNull("overall"));
        node.put("grade", score.grade().map(Enum::name).orElse(null));
        node.put("assessedCategories", score.assessedCategoryCount());
        node.put("totalCategories", Category.values().length);
        return node;
    }

    private ArrayNode categories(Assessment assessment) {
        ArrayNode categories = mapper.createArrayNode();
        assessment.score().categories().forEach(category -> categories.add(category(category, assessment.result())));
        return categories;
    }

    private ObjectNode category(CategoryScore score, AssessmentResult result) {
        ObjectNode node = mapper.createObjectNode();
        node.put("name", score.category().name());
        node.put("assessed", score.assessed());
        score.score().ifPresentOrElse(value -> node.put("score", value), () -> node.putNull("score"));
        if (score.category() == Category.TESTING) {
            testingMetrics(result.reports()).ifPresent(metrics -> node.set("metrics", metrics));
        }
        if (!score.assessed()) {
            node.put("reason", inputIsMissingFor(score.category(), result) ? MISSING_INPUT : NOT_IMPLEMENTED);
        }
        return node;
    }

    private Optional<ObjectNode> testingMetrics(BuildReports reports) {
        Optional<Coverage> coverage = reports.coverage().content();
        Optional<TestResults> tests = reports.testResults().content();
        if (coverage.isEmpty() && tests.isEmpty()) {
            return Optional.empty();
        }
        ObjectNode metrics = mapper.createObjectNode();
        coverage.ifPresent(found -> putCoverage(metrics, found));
        tests.ifPresent(found -> putTests(metrics, found));
        return Optional.of(metrics);
    }

    private static void putCoverage(ObjectNode metrics, Coverage coverage) {
        metrics.put("lineCoverage", coverage.lines().percent());
        // A project without branches has no branch coverage to report, rather than a perfect one.
        if (coverage.branches().total() > 0) {
            metrics.put("branchCoverage", coverage.branches().percent());
        }
    }

    private static void putTests(ObjectNode metrics, TestResults tests) {
        metrics.put("testsPassed", tests.passed());
        metrics.put("testsFailed", tests.failed().size());
        metrics.put("testsSkipped", tests.skipped());
    }

    private ArrayNode findings(AssessmentResult result) {
        ArrayNode findings = mapper.createArrayNode();
        result.findings().forEach(finding -> findings.add(finding(finding)));
        return findings;
    }

    private ObjectNode finding(Finding finding) {
        ObjectNode node = mapper.createObjectNode();
        node.put("ruleId", finding.ruleId());
        node.put("severity", finding.severity().name());
        node.put("category", finding.category().name());
        node.put("message", finding.message());
        ObjectNode evidence = node.putObject("evidence");
        evidence.put("source", finding.evidence().source().name());
        evidence.put("location", finding.evidence().location());
        node.put("remediation", finding.remediation());
        node.put("docUrl", finding.docUrl());
        return node;
    }

    private ArrayNode failedRules(AssessmentResult result) {
        ArrayNode failedRules = mapper.createArrayNode();
        for (FailedRule failedRule : result.failedRules()) {
            ObjectNode node = failedRules.addObject();
            node.put("ruleId", failedRule.ruleId());
            node.put("error", failedRule.error());
        }
        return failedRules;
    }

    private static long blockerCount(AssessmentResult result) {
        return result.findings().stream().filter(finding -> finding.severity() == Severity.BLOCKER).count();
    }

    private static boolean inputIsMissingFor(Category category, AssessmentResult result) {
        return result.skippedRules().stream().anyMatch(skipped -> skipped.category() == category);
    }
}
