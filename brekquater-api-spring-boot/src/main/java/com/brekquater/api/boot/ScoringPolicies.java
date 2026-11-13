// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.Severity;
import com.brekquater.api.rules.scoring.Grade;
import com.brekquater.api.rules.scoring.ScoringPolicy;
import java.util.Map;

/** Turns the {@code brekquater.api.score.*} properties into the rules module's scoring policy. */
final class ScoringPolicies {

    private ScoringPolicies() {}

    static ScoringPolicy of(BrekquaterApiProperties.Score score) {
        BrekquaterApiProperties.Weights weights = score.weights();
        BrekquaterApiProperties.Grades grades = score.grades();
        return new ScoringPolicy(
                Map.of(
                        Severity.BLOCKER, weights.blocker(),
                        Severity.MAJOR, weights.major(),
                        Severity.MINOR, weights.minor(),
                        Severity.INFO, weights.info()),
                Map.of(Grade.A, grades.a(), Grade.B, grades.b(), Grade.C, grades.c(), Grade.D, grades.d()));
    }
}
