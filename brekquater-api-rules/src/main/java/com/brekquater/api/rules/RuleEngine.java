// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Optional;

/**
 * Runs the registered rules against the available inputs.
 *
 * <p>The engine is deterministic: rules run one after another in ID order and the findings are sorted, so the same
 * inputs always give the same result. A rule that throws is reported as failed and never affects the other rules.
 */
public final class RuleEngine {

    private static final String UNPACKAGED_VERSION = "development";

    private final RuleRegistry registry;

    public RuleEngine(RuleRegistry registry) {
        this.registry = registry;
    }

    public AssessmentResult assess(AssessmentContext context) {
        AssessmentResult.Builder result = AssessmentResult.builder(ruleSetVersion());
        context.openApi().map(OpenApiContext::document).ifPresent(result::document);
        result.reports(context.reports());
        RuleRun run = new RuleRun(context, result);
        registry.rules().forEach(run::evaluate);
        return result.build();
    }

    // The version comes from the jar manifest, which does not exist when the classes run unpackaged, as in tests.
    private static String ruleSetVersion() {
        return Optional.ofNullable(RuleEngine.class.getPackage().getImplementationVersion())
                .orElse(UNPACKAGED_VERSION);
    }
}
