// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.brekquater.api.rules.Severity;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ScoringPolicyTest {

    private final ScoringPolicy policy = ScoringPolicy.defaults();

    @ParameterizedTest
    @CsvSource({"100, A", "90, A", "89, B", "75, B", "74, C", "60, C", "59, D", "40, D", "39, F", "0, F"})
    void gradesAScoreByTheDefaultBands(int score, Grade expected) {
        assertThat(policy.gradeOf(score)).isEqualTo(expected);
    }

    @Test
    void usesTheOpenSsfRiskWeightsByDefault() {
        assertThat(policy.weightOf(Severity.BLOCKER)).isEqualByComparingTo("10");
        assertThat(policy.weightOf(Severity.MAJOR)).isEqualByComparingTo("7.5");
        assertThat(policy.weightOf(Severity.MINOR)).isEqualByComparingTo("5");
        assertThat(policy.weightOf(Severity.INFO)).isEqualByComparingTo("0");
    }

    @Test
    void givesASeverityWithoutAConfiguredWeightNoWeight() {
        ScoringPolicy majorOnly = policy.withWeights(Map.of(Severity.MAJOR, BigDecimal.ONE));

        assertThat(majorOnly.weightOf(Severity.MINOR)).isEqualByComparingTo("0");
    }

    @Test
    void rejectsANegativeWeight() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> policy.withWeights(Map.of(Severity.MAJOR, new BigDecimal("-1"))));
    }
}
