// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;

class RuleResultTest {

    private static final Rule RULE = new ThrowingRule("BQ-TST-901");
    private static final Explanation EXPLANATION = new Explanation("Something is wrong.", "Fix it.");

    @Test
    void aRuleWithNothingToCheckHasNoTargetsAndNoFindings() {
        assertThat(RuleResult.notApplicable()).isEqualTo(RuleResult.of(0, List.of()));
    }

    @Test
    void rejectsANegativeNumberOfTargets() {
        assertThatIllegalArgumentException().isThrownBy(() -> RuleResult.of(-1, List.of()));
    }

    @Test
    void everyTargetPassesWhenThereAreNoFindings() {
        assertThat(RuleResult.of(4, List.of()).passingTargets()).isEqualTo(4);
    }

    @Test
    void twoFindingsAtTheSamePlaceFailOneTarget() {
        RuleResult result = RuleResult.of(4, List.of(findingAt("/a"), findingAt("/a"), findingAt("/b")));

        assertThat(result.passingTargets()).isEqualTo(2);
    }

    @Test
    void theNumberOfPassingTargetsIsNeverNegative() {
        RuleResult result = RuleResult.of(1, List.of(findingAt("/a"), findingAt("/b")));

        assertThat(result.passingTargets()).isZero();
    }

    private static Finding findingAt(String location) {
        return Finding.of(RULE, new Evidence(RuleInput.OPENAPI, location), EXPLANATION);
    }
}
