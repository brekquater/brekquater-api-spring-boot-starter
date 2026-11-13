// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The rules that take part in an assessment (Registry pattern). It guarantees that rule IDs are unique and keeps
 * the rules ordered by ID, which makes the engine's evaluation order deterministic.
 */
public final class RuleRegistry {

    private final List<Rule> rules;

    public RuleRegistry(Collection<? extends Rule> rules) {
        this.rules = rules.stream().sorted(Comparator.comparing(Rule::id)).map(Rule.class::cast).toList();
        rejectDuplicateIds();
    }

    /** Returns the rules registered in {@code META-INF/services/com.brekquater.api.rules.Rule} on the classpath. */
    public static RuleRegistry fromServiceLoader() {
        return new RuleRegistry(ServiceLoader.load(Rule.class).stream()
                .map(ServiceLoader.Provider::get)
                .toList());
    }

    public List<Rule> rules() {
        return rules;
    }

    /** Returns the IDs of all registered rules, sorted. */
    public Set<String> ids() {
        return rules.stream().map(Rule::id).collect(Collectors.toCollection(TreeSet::new));
    }

    /** Returns those of the given IDs that match no registered rule, sorted. */
    public Set<String> unknown(Collection<String> ruleIds) {
        Set<String> unknown = new TreeSet<>(ruleIds);
        unknown.removeAll(ids());
        return unknown;
    }

    /** Returns a registry without the given rules. An ID that matches no rule is rejected, to catch typos. */
    public RuleRegistry without(Set<String> disabledRuleIds) {
        rejectUnknown(disabledRuleIds);
        return new RuleRegistry(rules.stream()
                .filter(rule -> !disabledRuleIds.contains(rule.id()))
                .toList());
    }

    /**
     * Returns a registry in which the named rules report with the given severity. Only security rules can be raised
     * to {@link Severity#BLOCKER}, because strict mode fails startup on blockers.
     */
    public RuleRegistry withSeverities(Map<String, Severity> severities) {
        rejectUnknown(severities.keySet());
        return new RuleRegistry(rules.stream()
                .map(rule -> severities.containsKey(rule.id()) ? new SeverityOverride(rule, severities.get(rule.id())) : rule)
                .toList());
    }

    private void rejectUnknown(Collection<String> ruleIds) {
        Set<String> unknown = unknown(ruleIds);
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Unknown rule IDs " + unknown + ". Known rule IDs: " + ids());
        }
    }

    private void rejectDuplicateIds() {
        Set<String> seen = new TreeSet<>();
        Set<String> duplicates =
                rules.stream().map(Rule::id).filter(id -> !seen.add(id)).collect(Collectors.toCollection(TreeSet::new));
        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException("Rule IDs must be unique, but these are used more than once: " + duplicates);
        }
    }
}
