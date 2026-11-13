// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.scoring;

import com.brekquater.api.rules.Severity;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

/**
 * The numbers that turn pass rates into a score and a grade.
 *
 * @param weights how much a rule of each severity counts; a severity with no entry or a zero weight is not scored
 * @param gradeMinimums the lowest score that still earns each grade; a score below all of them earns {@link Grade#F}
 */
public record ScoringPolicy(Map<Severity, BigDecimal> weights, Map<Grade, Integer> gradeMinimums) {

    // The OpenSSF Scorecard risk weights for Critical, High and Medium. INFO rules inform and are not scored.
    private static final Map<Severity, BigDecimal> DEFAULT_WEIGHTS = Map.of(
            Severity.BLOCKER, new BigDecimal("10"),
            Severity.MAJOR, new BigDecimal("7.5"),
            Severity.MINOR, new BigDecimal("5"),
            Severity.INFO, BigDecimal.ZERO);

    private static final Map<Grade, Integer> DEFAULT_GRADE_MINIMUMS =
            Map.of(Grade.A, 90, Grade.B, 75, Grade.C, 60, Grade.D, 40);

    public ScoringPolicy {
        weights = Map.copyOf(weights);
        gradeMinimums = Map.copyOf(gradeMinimums);
        rejectNegativeWeights(weights);
    }

    public static ScoringPolicy defaults() {
        return new ScoringPolicy(DEFAULT_WEIGHTS, DEFAULT_GRADE_MINIMUMS);
    }

    /** Returns a policy with other weights and the same grades. */
    public ScoringPolicy withWeights(Map<Severity, BigDecimal> otherWeights) {
        return new ScoringPolicy(otherWeights, gradeMinimums);
    }

    public BigDecimal weightOf(Severity severity) {
        return weights.getOrDefault(severity, BigDecimal.ZERO);
    }

    public Grade gradeOf(int score) {
        return Arrays.stream(Grade.values())
                .filter(grade -> gradeMinimums.containsKey(grade) && score >= gradeMinimums.get(grade))
                .findFirst()
                .orElse(Grade.F);
    }

    private static void rejectNegativeWeights(Map<Severity, BigDecimal> weights) {
        if (weights.values().stream().anyMatch(weight -> weight.signum() < 0)) {
            throw new IllegalArgumentException("Score weights must not be negative: " + weights);
        }
    }
}
