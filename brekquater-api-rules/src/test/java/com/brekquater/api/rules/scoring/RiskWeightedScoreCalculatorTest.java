// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.RuleOutcome;
import com.brekquater.api.rules.Severity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class RiskWeightedScoreCalculatorTest {

    private static final int OPERATIONS = 10;

    private final ScoreCalculator calculator = ScoreCalculator.riskWeighted(ScoringPolicy.defaults());

    @Test
    void scoresTheWorkedExampleOfTheRequirementsWithItsOriginalWeights() {
        ScoringPolicy threeToOne = ScoringPolicy.defaults()
                .withWeights(Map.of(Severity.MAJOR, new BigDecimal("3"), Severity.MINOR, BigDecimal.ONE));

        Score score = ScoreCalculator.riskWeighted(threeToOne).score(workedExample());

        assertThat(score.overall()).hasValue(70);
        assertThat(score.grade()).contains(Grade.C);
    }

    @Test
    void scoresTheWorkedExampleWithTheDefaultWeights() {
        Score score = calculator.score(workedExample());

        assertThat(score.overall()).hasValue(74);
        assertThat(score.grade()).contains(Grade.C);
    }

    @Test
    void givesFullMarksWhenEveryTargetPasses() {
        Score score = calculator.score(resultOf(outcome(Severity.MAJOR, Category.DESIGN, OPERATIONS)));

        assertThat(score.overall()).hasValue(100);
        assertThat(score.grade()).contains(Grade.A);
    }

    @Test
    void givesZeroWhenEveryTargetFails() {
        Score score = calculator.score(resultOf(outcome(Severity.MAJOR, Category.DESIGN, 0)));

        assertThat(score.overall()).hasValue(0);
        assertThat(score.grade()).contains(Grade.F);
    }

    @Test
    void givesNoScoreWhenNoRuleHadAnythingToCheck() {
        RuleOutcome nothingToCheck = new RuleOutcome("BQ-TST-901", Severity.MAJOR, Category.DESIGN, 0, 0);

        Score score = calculator.score(resultOf(nothingToCheck));

        assertThat(score.overall()).isEmpty();
        assertThat(score.grade()).isEmpty();
        assertThat(score.categories()).contains(new CategoryScore(Category.DESIGN, true, OptionalInt.empty()));
    }

    @Test
    void leavesInformationalRulesOutOfTheScore() {
        Score score = calculator.score(resultOf(
                outcome(Severity.INFO, Category.DESIGN, 0), outcome(Severity.MINOR, Category.DESIGN, OPERATIONS)));

        assertThat(score.overall()).hasValue(100);
    }

    @Test
    void countsBlockerRulesInTheScore() {
        Score score = calculator.score(resultOf(outcome(Severity.BLOCKER, Category.SECURITY, 9)));

        assertThat(score.overall()).hasValue(90);
    }

    @Test
    void weighsABlockerRuleMoreThanAMinorRule() {
        Score score = calculator.score(resultOf(
                outcome(Severity.BLOCKER, Category.SECURITY, 0), outcome(Severity.MINOR, Category.SECURITY, OPERATIONS)));

        assertThat(score.overall()).hasValue(33);
    }

    @Test
    void averagesOnlyTheCategoriesThatHaveAScore() {
        Score score = calculator.score(resultOf(
                outcome(Severity.MAJOR, Category.DESIGN, OPERATIONS), outcome(Severity.MAJOR, Category.DOCUMENTATION, 5)));

        assertThat(score.overall()).hasValue(75);
        assertThat(score.assessedCategoryCount()).isEqualTo(2);
    }

    @Test
    void listsEveryCategoryAndMarksTheUnassessedOnes() {
        Score score = calculator.score(resultOf(outcome(Severity.MAJOR, Category.DESIGN, OPERATIONS)));

        assertThat(score.categories()).extracting(CategoryScore::category).containsExactly(Category.values());
        assertThat(score.categories())
                .contains(
                        new CategoryScore(Category.DESIGN, true, OptionalInt.of(100)),
                        new CategoryScore(Category.TESTING, false, OptionalInt.empty()));
    }

    @Test
    void roundsHalfUpAndOnlyOnce() {
        RuleOutcome oneOfEight = new RuleOutcome("BQ-TST-901", Severity.MAJOR, Category.DESIGN, 8, 1);

        assertThat(calculator.score(resultOf(oneOfEight)).overall()).hasValue(13);
    }

    // Ten operations; rule A (MAJOR) passes on 8, rule B (MINOR) on 10 and rule C (MAJOR) on 5.
    private static AssessmentResult workedExample() {
        return resultOf(
                outcome(Severity.MAJOR, Category.DESIGN, 8),
                outcome(Severity.MINOR, Category.DESIGN, 10),
                outcome(Severity.MAJOR, Category.DESIGN, 5));
    }

    private static RuleOutcome outcome(Severity severity, Category category, int passingOperations) {
        return new RuleOutcome("BQ-TST-901", severity, category, OPERATIONS, passingOperations);
    }

    private static AssessmentResult resultOf(RuleOutcome... outcomes) {
        return new AssessmentResult("test", Optional.empty(), List.of(), List.of(outcomes), List.of(), List.of());
    }
}
