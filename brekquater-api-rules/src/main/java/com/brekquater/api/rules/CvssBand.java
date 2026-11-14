// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The qualitative severity bands of CVSS v3.1 and v4.0, which share the same score ranges. Scanners report a score,
 * a band name, or both; each maps to one band.
 */
public enum CvssBand {
    CRITICAL("9.0"),
    HIGH("7.0"),
    MEDIUM("4.0"),
    LOW("0.1"),
    NONE("0.0");

    // GitHub advisories call the medium band "moderate".
    private static final String GITHUB_MEDIUM = "MODERATE";

    private final BigDecimal lowestScore;

    CvssBand(String lowestScore) {
        this.lowestScore = new BigDecimal(lowestScore);
    }

    /** Returns the band a CVSS base score falls in. */
    public static CvssBand of(BigDecimal score) {
        return Arrays.stream(values())
                .filter(band -> score.compareTo(band.lowestScore) >= 0)
                .findFirst()
                .orElse(NONE);
    }

    /** Returns the band with the given name, ignoring case; any other name, such as "unknown", gives empty. */
    public static Optional<CvssBand> named(String name) {
        String upperCase = name.trim().toUpperCase(Locale.ROOT);
        String bandName = upperCase.equals(GITHUB_MEDIUM) ? MEDIUM.name() : upperCase;
        return Arrays.stream(values()).filter(band -> band.name().equals(bandName)).findFirst();
    }

    /** Returns the name for people, such as "High". */
    public String label() {
        return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }
}
