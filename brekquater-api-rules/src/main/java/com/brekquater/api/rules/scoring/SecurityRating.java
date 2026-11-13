// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.scoring;

import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Finding;
import com.brekquater.api.rules.Severity;
import java.util.Comparator;
import java.util.Optional;

/**
 * Letter set by the single worst open security finding, after the SonarQube security rating. An average can hide
 * one serious issue among many passing targets; this rating cannot.
 *
 * <p>{@code D} exists to keep the familiar A to E scale but is never assigned, because Brekquater has four
 * severities where SonarQube has five.
 */
public enum SecurityRating {
    /** No security finding, or informational ones only. */
    A,
    /** At least one MINOR security finding. */
    B,
    /** At least one MAJOR security finding. */
    C,
    D,
    /** At least one BLOCKER security finding. */
    E;

    /** Returns the rating of the result, or empty when no security rule was evaluated. */
    public static Optional<SecurityRating> of(AssessmentResult result) {
        if (!result.assessedCategories().contains(Category.SECURITY)) {
            return Optional.empty();
        }
        Optional<Severity> worst = result.findings().stream()
                .filter(finding -> finding.category() == Category.SECURITY)
                .map(Finding::severity)
                .min(Comparator.naturalOrder());
        return Optional.of(worst.map(SecurityRating::ratingFor).orElse(A));
    }

    private static SecurityRating ratingFor(Severity worst) {
        return switch (worst) {
            case BLOCKER -> E;
            case MAJOR -> C;
            case MINOR -> B;
            case INFO -> A;
        };
    }
}
