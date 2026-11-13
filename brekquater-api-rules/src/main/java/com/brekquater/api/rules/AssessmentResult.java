// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Everything one run of the rule engine produced. Findings are always in {@link Finding#REPORT_ORDER}, however the
 * result was built, so that the same inputs give the same report.
 *
 * @param ruleSetVersion the version of the rules that produced the result
 * @param document the assessed OpenAPI document, or empty when there was none
 * @param findings every finding that was not suppressed
 * @param outcomes the pass rate of each evaluated rule
 * @param failedRules the rules that threw
 * @param skippedRules the rules whose input was absent
 */
public record AssessmentResult(
        String ruleSetVersion,
        Optional<DocumentSummary> document,
        List<Finding> findings,
        List<RuleOutcome> outcomes,
        List<FailedRule> failedRules,
        List<SkippedRule> skippedRules) {

    public AssessmentResult {
        findings = findings.stream().sorted(Finding.REPORT_ORDER).toList();
        outcomes = List.copyOf(outcomes);
        failedRules = List.copyOf(failedRules);
        skippedRules = List.copyOf(skippedRules);
    }

    /** Returns the categories in which at least one rule was evaluated. */
    public Set<Category> assessedCategories() {
        Set<Category> assessed =
                outcomes.stream().map(RuleOutcome::category).collect(Collectors.toCollection(Builder::noCategories));
        return Collections.unmodifiableSet(assessed);
    }

    static Builder builder(String ruleSetVersion) {
        return new Builder(ruleSetVersion);
    }

    /** Collects what the engine learns rule by rule (Builder pattern). */
    static final class Builder {

        private final String ruleSetVersion;
        private final List<Finding> findings = new ArrayList<>();
        private final List<RuleOutcome> outcomes = new ArrayList<>();
        private final List<FailedRule> failedRules = new ArrayList<>();
        private final List<SkippedRule> skippedRules = new ArrayList<>();
        private Optional<DocumentSummary> document = Optional.empty();

        private Builder(String ruleSetVersion) {
            this.ruleSetVersion = ruleSetVersion;
        }

        void document(DocumentSummary summary) {
            document = Optional.of(summary);
        }

        void evaluated(Rule rule, RuleResult result) {
            findings.addAll(result.findings());
            outcomes.add(RuleOutcome.of(rule, result));
        }

        void failed(Rule rule, Throwable thrown) {
            failedRules.add(FailedRule.of(rule, thrown));
        }

        void skipped(Rule rule) {
            skippedRules.add(SkippedRule.of(rule));
        }

        AssessmentResult build() {
            return new AssessmentResult(ruleSetVersion, document, findings, outcomes, failedRules, skippedRules);
        }

        private static Set<Category> noCategories() {
            return EnumSet.noneOf(Category.class);
        }
    }
}
