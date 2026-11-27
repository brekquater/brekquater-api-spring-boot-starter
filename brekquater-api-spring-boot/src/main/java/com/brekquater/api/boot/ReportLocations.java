// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Where to look for each build report. A report that is not configured is looked for where Maven writes it,
 * relative to the working directory: the project directory under {@code ./mvnw spring-boot:run} and in IDEs.
 *
 * @param coverage the JaCoCo XML report
 * @param testResults files, or directories of {@code TEST-*.xml} files, that are all read
 * @param vulnerabilities candidate vulnerability reports, of which the first that exists is read
 * @param baseline the OpenAPI document of the last release
 */
record ReportLocations(
        Optional<Path> coverage, List<Path> testResults, List<Path> vulnerabilities, Optional<Path> baseline) {

    /** Looks nowhere, so that no build report is ever found. */
    static final ReportLocations NONE = new ReportLocations(Optional.empty(), List.of(), List.of(), Optional.empty());

    private static final Path MAVEN_COVERAGE = Path.of("target", "site", "jacoco", "jacoco.xml");
    private static final List<Path> MAVEN_TEST_RESULTS =
            List.of(Path.of("target", "surefire-reports"), Path.of("target", "failsafe-reports"));
    private static final List<Path> MAVEN_VULNERABILITIES =
            List.of(Path.of("target", "osv-scanner.json"), Path.of("target", "dependency-check-report.json"));
    private static final Path BASELINE = Path.of("openapi-baseline.json");

    ReportLocations {
        testResults = List.copyOf(testResults);
        vulnerabilities = List.copyOf(vulnerabilities);
    }

    static ReportLocations of(BrekquaterApiProperties.Reports reports) {
        return new ReportLocations(
                Optional.of(reports.coverage().isBlank() ? MAVEN_COVERAGE : Path.of(reports.coverage())),
                reports.testResults().isEmpty() ? MAVEN_TEST_RESULTS : paths(reports.testResults()),
                reports.vulnerabilities().isBlank() ? MAVEN_VULNERABILITIES : paths(List.of(reports.vulnerabilities())),
                Optional.of(reports.baseline().isBlank() ? BASELINE : Path.of(reports.baseline())));
    }

    private static List<Path> paths(List<String> locations) {
        return locations.stream().map(Path::of).toList();
    }
}
