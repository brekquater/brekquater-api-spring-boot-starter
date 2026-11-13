// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;

/**
 * Outcome of evaluating one rule. It carries the number of targets the rule applied to, because the pass-rate
 * score cannot be computed from the findings alone.
 *
 * @param applicableTargets how many targets the rule checked; zero excludes the rule from the score
 * @param findings one finding per failing target
 */
public record RuleResult(int applicableTargets, List<Finding> findings) {

    private static final RuleResult NOT_APPLICABLE = new RuleResult(0, List.of());

    public RuleResult {
        if (applicableTargets < 0) {
            throw new IllegalArgumentException("applicableTargets must not be negative: " + applicableTargets);
        }
        findings = List.copyOf(findings);
    }

    /** Returns the result of a rule that found nothing to check. */
    public static RuleResult notApplicable() {
        return NOT_APPLICABLE;
    }

    public static RuleResult of(int applicableTargets, List<Finding> findings) {
        return new RuleResult(applicableTargets, findings);
    }

    /** Returns how many targets passed: the applicable targets minus the distinct failing ones. */
    public int passingTargets() {
        long failingTargets = findings.stream().map(Finding::evidence).distinct().count();
        return (int) Math.max(0, applicableTargets - failingTargets);
    }
}
