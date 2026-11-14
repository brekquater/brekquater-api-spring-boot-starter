// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

/**
 * The coverage a project is expected to reach, in whole percent.
 *
 * @param lineCoverage the lowest acceptable line coverage
 * @param branchCoverage the lowest acceptable branch coverage
 */
public record CoverageTargets(int lineCoverage, int branchCoverage) {

    /** Line coverage of 80%, as in the Sonar way quality gate, and branch coverage of 70%. */
    public static final CoverageTargets DEFAULTS = new CoverageTargets(80, 70);

    private static final int MAXIMUM = 100;

    public CoverageTargets {
        requirePercent("lineCoverage", lineCoverage);
        requirePercent("branchCoverage", branchCoverage);
    }

    private static void requirePercent(String name, int value) {
        if (value < 0 || value > MAXIMUM) {
            throw new IllegalArgumentException(name + " must be between 0 and 100: " + value);
        }
    }
}
