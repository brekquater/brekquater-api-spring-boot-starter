// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;
import java.util.Optional;

/**
 * Everything the rules may read during one assessment. An input that could not be obtained is empty, never null.
 *
 * @param openApi the application's OpenAPI document
 * @param reports the reports from the application's build
 * @param coverageTargets the coverage the coverage rules expect
 */
public record AssessmentContext(
        Optional<OpenApiContext> openApi, BuildReports reports, CoverageTargets coverageTargets) {

    private static final AssessmentContext EMPTY =
            new AssessmentContext(Optional.empty(), BuildReports.none(), CoverageTargets.DEFAULTS);

    public AssessmentContext {
        Objects.requireNonNull(openApi, "openApi");
        Objects.requireNonNull(reports, "reports");
        Objects.requireNonNull(coverageTargets, "coverageTargets");
    }

    /** Returns a context with only the OpenAPI document and the default coverage targets. */
    public static AssessmentContext of(OpenApiContext openApi) {
        return EMPTY.withOpenApi(openApi);
    }

    /** Returns a context with no inputs, for example when the application has no OpenAPI document. */
    public static AssessmentContext empty() {
        return EMPTY;
    }

    public AssessmentContext withOpenApi(OpenApiContext document) {
        return new AssessmentContext(Optional.of(document), reports, coverageTargets);
    }

    public AssessmentContext withReports(BuildReports buildReports) {
        return new AssessmentContext(openApi, buildReports, coverageTargets);
    }

    public AssessmentContext withCoverageTargets(CoverageTargets targets) {
        return new AssessmentContext(openApi, reports, targets);
    }

    public Optional<Coverage> coverage() {
        return reports.coverage().content();
    }

    public Optional<TestResults> testResults() {
        return reports.testResults().content();
    }

    public Optional<Vulnerabilities> vulnerabilities() {
        return reports.vulnerabilities().content();
    }

    /** Returns how the document changed since the baseline, when both documents were read. */
    public Optional<ApiChanges> changes() {
        return reports.baseline().content();
    }

    public boolean provides(RuleInput input) {
        return switch (input) {
            case OPENAPI -> openApi.isPresent();
            case COVERAGE -> coverage().isPresent();
            case TEST_RESULTS -> testResults().isPresent();
            case VULNERABILITIES -> vulnerabilities().isPresent();
            case BASELINE -> changes().isPresent();
        };
    }
}
