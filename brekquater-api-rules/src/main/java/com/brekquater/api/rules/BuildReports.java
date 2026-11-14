// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;

/**
 * The reports from the application's build that rules may read besides the OpenAPI document.
 *
 * @param coverage the JaCoCo coverage report
 * @param testResults the Surefire and Failsafe test reports, combined
 * @param vulnerabilities the dependency vulnerability report
 */
public record BuildReports(
        BuildReport<Coverage> coverage,
        BuildReport<TestResults> testResults,
        BuildReport<Vulnerabilities> vulnerabilities) {

    private static final BuildReports NONE =
            new BuildReports(BuildReport.missing(), BuildReport.missing(), BuildReport.missing());

    public BuildReports {
        Objects.requireNonNull(coverage, "coverage");
        Objects.requireNonNull(testResults, "testResults");
        Objects.requireNonNull(vulnerabilities, "vulnerabilities");
    }

    /** Returns the state in which no build report was found. */
    public static BuildReports none() {
        return NONE;
    }
}
