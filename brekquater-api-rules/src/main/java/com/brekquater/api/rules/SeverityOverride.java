// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;
import java.util.Objects;

/**
 * A rule run with a different severity than its author chose (Decorator pattern). Everything else, including the
 * targets it checks, stays the same.
 */
final class SeverityOverride implements Rule {

    private final Rule rule;
    private final Severity severity;

    SeverityOverride(Rule rule, Severity severity) {
        this.rule = Objects.requireNonNull(rule, "rule");
        this.severity = Objects.requireNonNull(severity, "severity");
        if (severity == Severity.BLOCKER && rule.category() != Category.SECURITY) {
            throw new IllegalArgumentException(
                    "Only security rules can be raised to BLOCKER, but " + rule.id() + " is a " + rule.category()
                            + " rule.");
        }
    }

    @Override
    public String id() {
        return rule.id();
    }

    @Override
    public Severity severity() {
        return severity;
    }

    @Override
    public Category category() {
        return rule.category();
    }

    @Override
    public RuleInput input() {
        return rule.input();
    }

    @Override
    public RuleResult evaluate(AssessmentContext context) {
        RuleResult result = rule.evaluate(context);
        List<Finding> findings = result.findings().stream().map(this::withOverriddenSeverity).toList();
        return RuleResult.of(result.applicableTargets(), findings);
    }

    private Finding withOverriddenSeverity(Finding finding) {
        return new Finding(
                finding.ruleId(),
                severity,
                finding.category(),
                finding.message(),
                finding.evidence(),
                finding.remediation(),
                finding.docUrl());
    }
}
