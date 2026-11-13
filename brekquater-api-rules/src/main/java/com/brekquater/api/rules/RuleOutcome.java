// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

/**
 * Pass rate of one evaluated rule. This is everything scoring needs, so a score never requires re-running rules.
 *
 * @param ruleId the ID of the evaluated rule
 * @param severity the rule's severity, which selects its weight
 * @param category the category the rule counts towards
 * @param applicableTargets how many targets the rule checked
 * @param passingTargets how many of them passed, after suppressions
 */
public record RuleOutcome(String ruleId, Severity severity, Category category, int applicableTargets, int passingTargets) {

    static RuleOutcome of(Rule rule, RuleResult result) {
        return new RuleOutcome(
                rule.id(), rule.severity(), rule.category(), result.applicableTargets(), result.passingTargets());
    }
}
