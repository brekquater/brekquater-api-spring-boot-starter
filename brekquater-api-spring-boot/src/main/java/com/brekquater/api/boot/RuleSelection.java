// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Rule;
import com.brekquater.api.rules.RuleRegistry;
import com.brekquater.api.rules.Severity;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/** Applies the {@code brekquater.api.rules.*} properties to the discovered rules, and explains any mistake in them. */
final class RuleSelection {

    private static final String DISABLED = "brekquater.api.rules.disabled";
    private static final String SEVERITY_OVERRIDES = "brekquater.api.rules.severity-overrides";

    private RuleSelection() {}

    static RuleRegistry of(RuleRegistry discovered, BrekquaterApiProperties.Rules properties) {
        rejectUnknown(discovered, properties.disabled(), DISABLED);
        rejectUnknown(discovered, properties.severityOverrides().keySet(), SEVERITY_OVERRIDES);
        rejectNonSecurityBlockers(discovered, properties.severityOverrides());
        return discovered.withSeverities(properties.severityOverrides()).without(properties.disabled());
    }

    private static void rejectUnknown(RuleRegistry discovered, Collection<String> ruleIds, String property) {
        Set<String> unknown = discovered.unknown(ruleIds);
        if (!unknown.isEmpty()) {
            throw new InvalidBrekquaterConfigurationException(
                    property, unknown, "Use the IDs of existing rules: " + discovered.ids() + ".");
        }
    }

    private static void rejectNonSecurityBlockers(RuleRegistry discovered, Map<String, Severity> overrides) {
        discovered.rules().stream()
                .filter(rule -> overrides.get(rule.id()) == Severity.BLOCKER)
                .filter(rule -> rule.category() != Category.SECURITY)
                .map(Rule::id)
                .findFirst()
                .ifPresent(ruleId -> {
                    throw new InvalidBrekquaterConfigurationException(
                            SEVERITY_OVERRIDES + "." + ruleId,
                            Severity.BLOCKER,
                            "BLOCKER is reserved for security rules. Use MAJOR, MINOR or INFO.");
                });
    }
}
