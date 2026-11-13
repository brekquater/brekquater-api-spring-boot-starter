// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.AssessmentContext;
import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.RuleEngine;
import com.brekquater.api.rules.openapi.SwaggerOpenApiContext;
import com.brekquater.api.rules.report.Assessment;
import com.brekquater.api.rules.scoring.ScoreCalculator;
import java.time.Clock;
import java.util.Optional;

/** Assesses a document by running the rule engine and scoring the result. */
final class EngineAssessmentService implements AssessmentService {

    private final RuleEngine engine;
    private final ScoreCalculator calculator;
    private final Clock clock;

    EngineAssessmentService(RuleEngine engine, ScoreCalculator calculator, Clock clock) {
        this.engine = engine;
        this.calculator = calculator;
        this.clock = clock;
    }

    @Override
    public Assessment assess(Optional<OpenApiDocument> document) {
        AssessmentContext context = document
                .flatMap(OpenApiDocumentParser::parse)
                .map(openApi -> AssessmentContext.of(new SwaggerOpenApiContext(openApi)))
                .orElse(AssessmentContext.empty());
        AssessmentResult result = engine.assess(context);
        return new Assessment(result, calculator.score(result), clock.instant());
    }
}
