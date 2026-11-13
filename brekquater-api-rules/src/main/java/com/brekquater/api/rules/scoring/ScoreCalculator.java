// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.scoring;

import com.brekquater.api.rules.AssessmentResult;

/** Turns the pass rates of an assessment into a score (Strategy pattern). */
public interface ScoreCalculator {

    Score score(AssessmentResult result);

    /**
     * Returns the default calculator: a severity-weighted pass rate per category, then the mean of the categories
     * that have a score. Categories that were not assessed are left out, never counted as full marks.
     */
    static ScoreCalculator riskWeighted(ScoringPolicy policy) {
        return new RiskWeightedScoreCalculator(policy);
    }
}
