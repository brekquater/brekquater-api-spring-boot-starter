// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.RuleEngine;
import com.brekquater.api.rules.report.Assessment;
import com.brekquater.api.rules.scoring.ScoreCalculator;
import java.time.Clock;
import java.util.Optional;

/** Assesses a document and the build reports by running the rule engine and scoring the result. */
final class EngineAssessmentService implements AssessmentService {

    private final AssessmentInputs inputs;
    private final RuleEngine engine;
    private final ScoreCalculator calculator;
    private final Clock clock;

    EngineAssessmentService(Collaborators collaborators) {
        this.inputs = collaborators.inputs();
        this.engine = collaborators.engine();
        this.calculator = collaborators.calculator();
        this.clock = collaborators.clock();
    }

    @Override
    public Assessment assess(Optional<OpenApiDocument> document) {
        AssessmentResult result = engine.assess(inputs.contextFor(document));
        return new Assessment(result, calculator.score(result), clock.instant());
    }

    /** What the service needs, grouped so its constructor stays small. */
    record Collaborators(AssessmentInputs inputs, RuleEngine engine, ScoreCalculator calculator, Clock clock) {}
}
