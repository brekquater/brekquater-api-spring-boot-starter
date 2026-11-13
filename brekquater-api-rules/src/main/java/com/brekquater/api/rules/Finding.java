// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Comparator;
import java.util.Objects;

/**
 * One failing target reported by a rule. Create findings with {@link #of(Rule, Evidence, Explanation)} (static
 * factory), which derives the identifying fields from the rule so they cannot drift.
 *
 * @param ruleId the stable ID of the rule that raised the finding
 * @param severity how serious the finding is
 * @param category the quality area the rule belongs to
 * @param message what is wrong
 * @param evidence where it was observed
 * @param remediation how to fix it
 * @param docUrl the rule's documentation page
 */
public record Finding(
        String ruleId,
        Severity severity,
        Category category,
        String message,
        Evidence evidence,
        String remediation,
        String docUrl) {

    /** Order of findings in every report: most serious first, then by rule ID, then by evidence. */
    public static final Comparator<Finding> REPORT_ORDER = Comparator.comparing(Finding::severity)
            .thenComparing(Finding::ruleId)
            .thenComparing(Finding::evidence);

    private static final String RULE_PAGES = "https://starter.brekquater.com/api/rules/";

    public Finding {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(evidence, "evidence");
        Objects.requireNonNull(remediation, "remediation");
        Objects.requireNonNull(docUrl, "docUrl");
    }

    /** Returns a finding raised by {@code rule} at {@code evidence}. */
    public static Finding of(Rule rule, Evidence evidence, Explanation explanation) {
        return new Finding(
                rule.id(),
                rule.severity(),
                rule.category(),
                explanation.message(),
                evidence,
                explanation.remediation(),
                docUrlOf(rule.id()));
    }

    /** Returns the address of the documentation page of the rule with the given ID. */
    public static String docUrlOf(String ruleId) {
        return RULE_PAGES + ruleId;
    }
}
