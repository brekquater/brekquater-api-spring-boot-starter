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
 * @param baseline how the OpenAPI document changed since the committed document of the last release
 */
public record BuildReports(
        BuildReport<Coverage> coverage,
        BuildReport<TestResults> testResults,
        BuildReport<Vulnerabilities> vulnerabilities,
        BuildReport<ApiChanges> baseline) {

    private static final BuildReports NONE = new BuildReports(
            BuildReport.missing(), BuildReport.missing(), BuildReport.missing(), BuildReport.missing());

    public BuildReports {
        Objects.requireNonNull(coverage, "coverage");
        Objects.requireNonNull(testResults, "testResults");
        Objects.requireNonNull(vulnerabilities, "vulnerabilities");
        Objects.requireNonNull(baseline, "baseline");
    }

    /** Returns the build reports without a baseline document. */
    public BuildReports(
            BuildReport<Coverage> coverage,
            BuildReport<TestResults> testResults,
            BuildReport<Vulnerabilities> vulnerabilities) {
        this(coverage, testResults, vulnerabilities, BuildReport.missing());
    }

    public BuildReports withBaseline(BuildReport<ApiChanges> changes) {
        return new BuildReports(coverage, testResults, vulnerabilities, changes);
    }

    /** Returns the state in which no build report was found. */
    public static BuildReports none() {
        return NONE;
    }
}
