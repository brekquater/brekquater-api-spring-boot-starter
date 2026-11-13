// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Evidence;
import com.brekquater.api.rules.Finding;
import com.brekquater.api.rules.RuleInput;
import com.brekquater.api.rules.RuleOutcome;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SecurityRatingTest {

    private static final RuleOutcome SECURITY_RULE_EVALUATED =
            new RuleOutcome("BQ-TST-901", Severity.BLOCKER, Category.SECURITY, 10, 10);
    private static final RuleOutcome DESIGN_RULE_EVALUATED =
            new RuleOutcome("BQ-TST-902", Severity.MAJOR, Category.DESIGN, 10, 10);

    @Test
    void thereIsNoRatingWhenNoSecurityRuleWasEvaluated() {
        AssessmentResult result = resultOf(DESIGN_RULE_EVALUATED, finding(Category.DESIGN, Severity.MAJOR));

        assertThat(SecurityRating.of(result)).isEmpty();
    }

    @Test
    void ratesAnApiWithoutSecurityFindingsA() {
        assertThat(SecurityRating.of(resultOf(SECURITY_RULE_EVALUATED))).contains(SecurityRating.A);
    }

    @Test
    void ratesInformationalSecurityFindingsA() {
        AssessmentResult result = resultOf(SECURITY_RULE_EVALUATED, finding(Category.SECURITY, Severity.INFO));

        assertThat(SecurityRating.of(result)).contains(SecurityRating.A);
    }

    @Test
    void ratesAMinorSecurityFindingB() {
        AssessmentResult result = resultOf(SECURITY_RULE_EVALUATED, finding(Category.SECURITY, Severity.MINOR));

        assertThat(SecurityRating.of(result)).contains(SecurityRating.B);
    }

    @Test
    void ratesAMajorSecurityFindingC() {
        AssessmentResult result = resultOf(SECURITY_RULE_EVALUATED, finding(Category.SECURITY, Severity.MAJOR));

        assertThat(SecurityRating.of(result)).contains(SecurityRating.C);
    }

    @Test
    void theWorstSecurityFindingSetsTheRating() {
        AssessmentResult result = resultOf(
                SECURITY_RULE_EVALUATED,
                finding(Category.SECURITY, Severity.MINOR),
                finding(Category.SECURITY, Severity.BLOCKER),
                finding(Category.SECURITY, Severity.MAJOR));

        assertThat(SecurityRating.of(result)).contains(SecurityRating.E);
    }

    @Test
    void findingsOfOtherCategoriesDoNotAffectTheRating() {
        AssessmentResult result = resultOf(SECURITY_RULE_EVALUATED, finding(Category.DESIGN, Severity.MAJOR));

        assertThat(SecurityRating.of(result)).contains(SecurityRating.A);
    }

    private static Finding finding(Category category, Severity severity) {
        Evidence evidence = new Evidence(RuleInput.OPENAPI, "/paths/~1orders/get");
        return new Finding("BQ-TST-901", severity, category, "Something is wrong.", evidence, "Fix it.", "https://x");
    }

    private static AssessmentResult resultOf(RuleOutcome outcome, Finding... findings) {
        return new AssessmentResult(
                "test", Optional.empty(), List.of(findings), List.of(outcome), List.of(), List.of());
    }
}
