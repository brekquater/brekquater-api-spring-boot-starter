// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;

/** One pass of the engine over the rules: evaluates a rule in isolation and records what happened. */
final class RuleRun {

    private final AssessmentContext context;
    private final Suppressions suppressions;
    private final AssessmentResult.Builder result;

    RuleRun(AssessmentContext context, AssessmentResult.Builder result) {
        this.context = context;
        this.suppressions = Suppressions.in(context);
        this.result = result;
    }

    void evaluate(Rule rule) {
        if (!context.provides(rule.input())) {
            result.skipped(rule);
            return;
        }
        try {
            result.evaluated(rule, withoutSuppressed(rule.evaluate(context)));
        } catch (RuntimeException | StackOverflowError thrown) {
            // A broken rule must never break the others or the application. StackOverflowError is included
            // because a rule that walks a self-referencing schema without a guard fails that way.
            result.failed(rule, thrown);
        }
    }

    // Dropping a suppressed finding also turns its target into a passing one, so an opt-out never lowers the score.
    private RuleResult withoutSuppressed(RuleResult ruleResult) {
        List<Finding> kept = ruleResult.findings().stream()
                .filter(finding -> !suppressions.suppresses(finding))
                .toList();
        return RuleResult.of(ruleResult.applicableTargets(), kept);
    }
}
