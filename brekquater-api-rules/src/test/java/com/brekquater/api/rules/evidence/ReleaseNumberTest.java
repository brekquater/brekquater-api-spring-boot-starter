// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReleaseNumberTest {

    @Test
    void comparesNumberByNumberRatherThanAsText() {
        assertThat(ReleaseNumber.of("2.12.4")).isGreaterThan(ReleaseNumber.of("2.3.2"));
        assertThat(ReleaseNumber.of("2.9.10.4")).isGreaterThan(ReleaseNumber.of("2.9.8"));
        assertThat(ReleaseNumber.of("10.0")).isGreaterThan(ReleaseNumber.of("9.99"));
    }

    @Test
    void treatsMissingNumbersAndLeadingZerosAsZero() {
        assertThat(ReleaseNumber.of("2.0")).isEqualByComparingTo(ReleaseNumber.of("2.0.0"));
        assertThat(ReleaseNumber.of("2.01")).isEqualByComparingTo(ReleaseNumber.of("2.1"));
    }

    @Test
    void ignoresQualifiersAfterTheNumbers() {
        assertThat(ReleaseNumber.of("2.0-beta9")).isEqualByComparingTo(ReleaseNumber.of("2.0"));
        assertThat(ReleaseNumber.of("5.3.39.Final")).isEqualByComparingTo(ReleaseNumber.of("5.3.39"));
    }

    @Test
    void handlesNumbersTooLongForALong() {
        assertThat(ReleaseNumber.of("1.20260101000000000000")).isGreaterThan(ReleaseNumber.of("1.9"));
    }
}
