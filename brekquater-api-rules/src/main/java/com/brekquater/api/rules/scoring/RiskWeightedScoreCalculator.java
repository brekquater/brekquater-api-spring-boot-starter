// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.scoring;

import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.RuleOutcome;
import com.brekquater.api.rules.Severity;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Pass-rate scoring in the style of the OpenSSF Scorecard aggregate: each rule scores the share of its targets
 * that pass, and rules are averaged with a weight that follows their severity.
 *
 * <p>A blocker caps the grade at F, whatever the score: one unprotected operation or critical vulnerability
 * outweighs any number of tidy ones, much as SSL Labs grades a server with a critical flaw F. The score itself is
 * kept, so progress on everything else stays visible.
 *
 * <p>A pass rate does not punish an API for being large, unlike a count of findings. All arithmetic is exact
 * decimal and is rounded only when a number is reported, so results are identical on every platform.
 */
final class RiskWeightedScoreCalculator implements ScoreCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final MathContext PRECISION = MathContext.DECIMAL64;

    private final ScoringPolicy policy;

    RiskWeightedScoreCalculator(ScoringPolicy policy) {
        this.policy = policy;
    }

    @Override
    public Score score(AssessmentResult result) {
        Map<Category, BigDecimal> exactScores = exactCategoryScores(result);
        Set<Category> assessed = result.assessedCategories();
        List<CategoryScore> categories = Arrays.stream(Category.values())
                .map(category -> new CategoryScore(
                        category, assessed.contains(category), roundedScoreOf(category, exactScores)))
                .toList();
        OptionalInt overall = mean(exactScores.values()).stream()
                .mapToInt(RiskWeightedScoreCalculator::rounded)
                .findFirst();
        Optional<Grade> grade = overall.stream().mapToObj(score -> gradeOf(score, result)).findFirst();
        return new Score(overall, grade, categories);
    }

    private Map<Category, BigDecimal> exactCategoryScores(AssessmentResult result) {
        Map<Category, List<RuleOutcome>> scoredByCategory = result.outcomes().stream()
                .filter(this::isScored)
                .collect(Collectors.groupingBy(RuleOutcome::category));
        Map<Category, BigDecimal> scores = new EnumMap<>(Category.class);
        scoredByCategory.forEach((category, outcomes) -> scores.put(category, weightedPassRate(outcomes)));
        return scores;
    }

    private boolean isScored(RuleOutcome outcome) {
        return outcome.applicableTargets() > 0 && weightOf(outcome).signum() > 0;
    }

    private BigDecimal weightedPassRate(List<RuleOutcome> outcomes) {
        BigDecimal weightedRates = sum(outcomes, outcome -> weightOf(outcome).multiply(passRate(outcome)));
        BigDecimal weights = sum(outcomes, this::weightOf);
        return weightedRates.multiply(HUNDRED).divide(weights, PRECISION);
    }

    private BigDecimal weightOf(RuleOutcome outcome) {
        return policy.weightOf(outcome.severity());
    }

    private Grade gradeOf(int overall, AssessmentResult result) {
        return hasBlocker(result) ? Grade.F : policy.gradeOf(overall);
    }

    private static boolean hasBlocker(AssessmentResult result) {
        return result.findings().stream().anyMatch(finding -> finding.severity() == Severity.BLOCKER);
    }

    private static BigDecimal passRate(RuleOutcome outcome) {
        BigDecimal passing = BigDecimal.valueOf(outcome.passingTargets());
        return passing.divide(BigDecimal.valueOf(outcome.applicableTargets()), PRECISION);
    }

    private static BigDecimal sum(List<RuleOutcome> outcomes, Function<RuleOutcome, BigDecimal> term) {
        return outcomes.stream().map(term).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // The mean uses the exact category scores, not the rounded ones, so rounding happens once.
    private static Optional<BigDecimal> mean(Collection<BigDecimal> scores) {
        if (scores.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal total = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(total.divide(BigDecimal.valueOf(scores.size()), PRECISION));
    }

    private static OptionalInt roundedScoreOf(Category category, Map<Category, BigDecimal> exactScores) {
        return Optional.ofNullable(exactScores.get(category)).stream()
                .mapToInt(RiskWeightedScoreCalculator::rounded)
                .findFirst();
    }

    private static int rounded(BigDecimal exactScore) {
        return exactScore.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }
}
