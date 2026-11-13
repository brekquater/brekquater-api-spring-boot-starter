// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RuleRegistryTest {

    private static final Rule FIRST = new ThrowingRule("BQ-TST-901");
    private static final Rule SECOND = new ThrowingRule("BQ-TST-902");

    @Test
    void keepsRulesOrderedById() {
        RuleRegistry registry = new RuleRegistry(List.of(SECOND, FIRST));

        assertThat(registry.rules()).containsExactly(FIRST, SECOND);
    }

    @Test
    void rejectsTwoRulesWithTheSameId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new RuleRegistry(List.of(FIRST, new ThrowingRule("BQ-TST-901"))))
                .withMessageContaining("BQ-TST-901");
    }

    @Test
    void leavesOutDisabledRules() {
        RuleRegistry registry = new RuleRegistry(List.of(FIRST, SECOND)).without(Set.of("BQ-TST-901"));

        assertThat(registry.rules()).containsExactly(SECOND);
    }

    @Test
    void rejectsADisabledIdThatMatchesNoRuleAndNamesTheKnownIds() {
        RuleRegistry registry = new RuleRegistry(List.of(FIRST, SECOND));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.without(Set.of("BQ-TYPO-001")))
                .withMessageContaining("BQ-TYPO-001")
                .withMessageContaining("BQ-TST-901", "BQ-TST-902");
    }

    @Test
    void namesTheIdsItDoesNotKnow() {
        RuleRegistry registry = new RuleRegistry(List.of(FIRST, SECOND));

        assertThat(registry.unknown(List.of("BQ-TST-902", "BQ-TYPO-002", "BQ-TYPO-001")))
                .containsExactly("BQ-TYPO-001", "BQ-TYPO-002");
        assertThat(registry.ids()).containsExactly("BQ-TST-901", "BQ-TST-902");
    }

    @Test
    void overridesTheSeverityOfARuleAndOfItsFindings() {
        Rule failing = new FailingAtRule("BQ-TST-903", Severity.MINOR, List.of("/a"));

        RuleRegistry registry = new RuleRegistry(List.of(failing)).withSeverities(Map.of("BQ-TST-903", Severity.MAJOR));

        Rule overridden = registry.rules().get(0);
        assertThat(overridden.severity()).isEqualTo(Severity.MAJOR);
        assertThat(overridden.id()).isEqualTo("BQ-TST-903");
        assertThat(overridden.category()).isEqualTo(failing.category());
        assertThat(overridden.input()).isEqualTo(failing.input());
        RuleResult result = overridden.evaluate(AssessmentContext.empty());
        assertThat(result.findings()).extracting(Finding::severity).containsExactly(Severity.MAJOR);
        assertThat(result.applicableTargets()).isEqualTo(FailingAtRule.APPLICABLE_TARGETS);
    }

    @Test
    void leavesRulesWithoutAnOverrideUnchanged() {
        RuleRegistry registry = new RuleRegistry(List.of(FIRST)).withSeverities(Map.of());

        assertThat(registry.rules()).containsExactly(FIRST);
    }

    @Test
    void refusesToRaiseANonSecurityRuleToBlocker() {
        RuleRegistry registry = new RuleRegistry(List.of(FIRST));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.withSeverities(Map.of("BQ-TST-901", Severity.BLOCKER)))
                .withMessageContaining("BQ-TST-901");
    }

    @Test
    void raisesASecurityRuleToBlocker() {
        RuleRegistry registry = new RuleRegistry(List.of(new SecurityRuleDouble("BQ-TST-904")))
                .withSeverities(Map.of("BQ-TST-904", Severity.BLOCKER));

        assertThat(registry.rules()).extracting(Rule::severity).containsExactly(Severity.BLOCKER);
    }

    @Test
    void rejectsAnOverrideForAnUnknownRule() {
        RuleRegistry registry = new RuleRegistry(List.of(FIRST));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.withSeverities(Map.of("BQ-TYPO-001", Severity.MAJOR)))
                .withMessageContaining("BQ-TYPO-001");
    }

    @Test
    void discoversTheBuiltInRules() {
        assertThat(RuleRegistry.fromServiceLoader().rules())
                .extracting(Rule::id)
                .hasSize(18)
                .contains("BQ-API-001", "BQ-DOC-001", "BQ-ERR-001", "BQ-SEC-001", "BQ-NAM-003");
    }
}
