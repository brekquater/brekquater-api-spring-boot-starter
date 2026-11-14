// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.BuildReports;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.RuleOutcome;
import com.brekquater.api.rules.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

class ScoreMonotonicityPropertyTest {

    private static final int MAXIMUM_RULES = 8;
    private static final int MAXIMUM_TARGETS = 50;

    private final ScoreCalculator calculator = ScoreCalculator.riskWeighted(ScoringPolicy.defaults());

    @Property
    void oneMoreFailureNeverRaisesTheScore(
            @ForAll("outcomesWithAPassingTarget") @Size(min = 1, max = MAXIMUM_RULES) List<RuleOutcome> outcomes,
            @ForAll @IntRange(max = MAXIMUM_RULES - 1) int position) {
        List<RuleOutcome> withOneMoreFailure = new ArrayList<>(outcomes);
        withOneMoreFailure.replaceAll(outcome -> outcome == outcomes.get(position % outcomes.size())
                ? new RuleOutcome(
                        outcome.ruleId(),
                        outcome.severity(),
                        outcome.category(),
                        outcome.applicableTargets(),
                        outcome.passingTargets() - 1)
                : outcome);

        assertThat(overallOf(withOneMoreFailure)).isLessThanOrEqualTo(overallOf(outcomes));
    }

    @Provide
    @SuppressWarnings("unused") // Called by jqwik through @ForAll("outcomesWithAPassingTarget").
    Arbitrary<List<RuleOutcome>> outcomesWithAPassingTarget() {
        Arbitrary<RuleOutcome> outcome = Arbitraries.integers()
                .between(1, MAXIMUM_TARGETS)
                .flatMap(applicable -> Combinators.combine(
                                Arbitraries.of(Severity.class),
                                Arbitraries.of(Category.class),
                                Arbitraries.integers().between(1, applicable))
                        .as((severity, category, passing) ->
                                new RuleOutcome("BQ-TST-901", severity, category, applicable, passing)));
        return outcome.list();
    }

    private int overallOf(List<RuleOutcome> outcomes) {
        AssessmentResult result =
                new AssessmentResult(
                        "test", Optional.empty(), BuildReports.none(), List.of(), outcomes, List.of(), List.of());
        return calculator.score(result).overall().orElse(0);
    }
}
