// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

/**
 * A rule the engine did not call because the artefact it reads was absent.
 *
 * @param ruleId the ID of the skipped rule
 * @param category the category that lost this rule's assessment
 * @param missingInput the artefact that was absent
 */
public record SkippedRule(String ruleId, Category category, RuleInput missingInput) {

    static SkippedRule of(Rule rule) {
        return new SkippedRule(rule.id(), rule.category(), rule.input());
    }
}
