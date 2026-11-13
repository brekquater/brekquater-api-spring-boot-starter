// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.catalogue.OperationHasSummaryOrDescription;
import com.brekquater.api.rules.openapi.OpenApiFixture;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleEngineTest {

    private static final AssessmentContext ORDERS_API = OpenApiFixture.context("""
            openapi: 3.1.0
            info: {title: Orders API, version: "1.0"}
            paths:
              /orders:
                get:
                  summary: List orders
                  responses: {'200': {description: OK}}
                post:
                  responses: {'201': {description: Created}}
            """);

    @Test
    void sortsFindingsBySeverityThenRuleIdThenEvidence() {
        RuleEngine engine = engineWith(
                new FailingAtRule("BQ-TST-902", Severity.MINOR, List.of("/b", "/a")),
                new FailingAtRule("BQ-TST-901", Severity.MINOR, List.of("/z")),
                new FailingAtRule("BQ-TST-903", Severity.MAJOR, List.of("/m")));

        AssessmentResult result = engine.assess(ORDERS_API);

        assertThat(result.findings())
                .extracting(finding -> finding.ruleId() + " " + finding.evidence().location())
                .containsExactly("BQ-TST-903 /m", "BQ-TST-901 /z", "BQ-TST-902 /a", "BQ-TST-902 /b");
    }

    @Test
    void reportsAThrowingRuleAsFailedWithoutLeakingItsMessage() {
        RuleEngine engine = engineWith(new ThrowingRule("BQ-TST-900"));

        AssessmentResult result = engine.assess(ORDERS_API);

        assertThat(result.failedRules())
                .containsExactly(new FailedRule("BQ-TST-900", "java.lang.IllegalStateException"));
    }

    @Test
    void aThrowingRuleDoesNotChangeWhatTheOtherRulesReport() {
        Rule healthy = new FailingAtRule("BQ-TST-901", Severity.MINOR, List.of("/a"));

        AssessmentResult alone = engineWith(healthy).assess(ORDERS_API);
        AssessmentResult besideABrokenRule =
                engineWith(new ThrowingRule("BQ-TST-900"), healthy).assess(ORDERS_API);

        assertThat(besideABrokenRule.findings()).isEqualTo(alone.findings());
        assertThat(besideABrokenRule.outcomes()).isEqualTo(alone.outcomes());
    }

    @Test
    void recordsThePassRateOfEachEvaluatedRule() {
        AssessmentResult result = engineWith(new OperationHasSummaryOrDescription()).assess(ORDERS_API);

        assertThat(result.outcomes())
                .containsExactly(new RuleOutcome("BQ-DOC-001", Severity.MINOR, Category.DOCUMENTATION, 2, 1));
        assertThat(result.assessedCategories()).containsExactly(Category.DOCUMENTATION);
    }

    @Test
    void dropsAFindingTheOperationOptsOutOfAndCountsItsTargetAsPassing() {
        AssessmentContext apiWithOptOut = OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    post:
                      x-brekquater-ignore: [BQ-DOC-001]
                      responses: {'201': {description: Created}}
                """);

        AssessmentResult result = engineWith(new OperationHasSummaryOrDescription()).assess(apiWithOptOut);

        assertThat(result.findings()).isEmpty();
        assertThat(result.outcomes())
                .containsExactly(new RuleOutcome("BQ-DOC-001", Severity.MINOR, Category.DOCUMENTATION, 1, 1));
    }

    @Test
    void anOptOutOnlyCoversTheRulesItNames() {
        AssessmentContext apiWithOtherOptOut = OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    post:
                      x-brekquater-ignore: [BQ-ERR-001]
                      responses: {'201': {description: Created}}
                """);

        AssessmentResult result = engineWith(new OperationHasSummaryOrDescription()).assess(apiWithOtherOptOut);

        assertThat(result.findings()).hasSize(1);
    }

    @Test
    void anOptOutAlsoCoversFindingsInsideTheOperation() {
        AssessmentContext apiWithOptOut = OpenApiFixture.context("""
                openapi: 3.1.0
                info: {title: Orders API, version: "1.0"}
                paths:
                  /orders:
                    post:
                      x-brekquater-ignore: [BQ-TST-901]
                      responses: {'201': {description: Created}}
                """);
        Rule rule = new FailingAtRule(
                "BQ-TST-901", Severity.MINOR, List.of("/paths/~1orders/post/responses/201", "/paths/~1orders/get"));

        AssessmentResult result = engineWith(rule).assess(apiWithOptOut);

        assertThat(result.findings())
                .extracting(finding -> finding.evidence().location())
                .containsExactly("/paths/~1orders/get");
    }

    @Test
    void skipsEveryRuleWhenThereIsNoOpenApiDocument() {
        AssessmentResult result =
                engineWith(new OperationHasSummaryOrDescription()).assess(AssessmentContext.empty());

        assertThat(result.skippedRules())
                .containsExactly(new SkippedRule("BQ-DOC-001", Category.DOCUMENTATION, RuleInput.OPENAPI));
        assertThat(result.outcomes()).isEmpty();
        assertThat(result.assessedCategories()).isEmpty();
        assertThat(result.document()).isEmpty();
    }

    @Test
    void describesTheAssessedDocument() {
        AssessmentResult result = engineWith().assess(ORDERS_API);

        assertThat(result.document()).contains(new DocumentSummary("3.1.0", "Orders API", "1.0", 2));
    }

    private static RuleEngine engineWith(Rule... rules) {
        return new RuleEngine(new RuleRegistry(List.of(rules)));
    }
}
