// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.report;

import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.scoring.Score;
import java.time.Instant;
import java.util.Objects;

/**
 * A finished assessment, ready to be reported.
 *
 * @param result what the rule engine found
 * @param score the score computed from the result
 * @param generatedAt when the assessment was made
 */
public record Assessment(AssessmentResult result, Score score, Instant generatedAt) {

    public Assessment {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(generatedAt, "generatedAt");
    }
}
