// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.ApiChanges;
import com.brekquater.api.rules.AssessmentContext;
import com.brekquater.api.rules.BuildReport;
import com.brekquater.api.rules.CoverageTargets;
import com.brekquater.api.rules.ReportStatus;
import com.brekquater.api.rules.openapi.SwaggerApiChanges;
import com.brekquater.api.rules.openapi.SwaggerOpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;

/** Gathers what one assessment reads: the OpenAPI document, the build reports and the coverage targets. */
final class AssessmentInputs {

    /** Reads only the document, never a build report. */
    static final AssessmentInputs DOCUMENT_ONLY =
            new AssessmentInputs(new BuildReportFiles(ReportLocations.NONE), CoverageTargets.DEFAULTS);

    // Absent documents share one key, so "no document" is cached like any other state.
    private static final String NO_DOCUMENT = "";

    private final BuildReportFiles reports;
    private final CoverageTargets targets;

    AssessmentInputs(BuildReportFiles reports, CoverageTargets targets) {
        this.reports = reports;
        this.targets = targets;
    }

    /** Returns a value that changes whenever the document or any build report changes. */
    String fingerprintOf(OpenApiDocument document) {
        return document.fingerprint() + "|" + reports.fingerprint();
    }

    /** Returns a value that changes whenever a build report changes, for an application without a document. */
    String fingerprintWithoutDocument() {
        return NO_DOCUMENT + "|" + reports.fingerprint();
    }

    AssessmentContext contextFor(OpenApiDocument document) {
        AssessmentContext context = contextWithoutDocument();
        return OpenApiDocumentParser.parse(document)
                .map(openApi -> context.withOpenApi(new SwaggerOpenApiContext(openApi))
                        .withReports(context.reports().withBaseline(changesSince(openApi))))
                .orElse(context);
    }

    // The comparison needs the current document, so it is made here rather than with the other reports.
    private BuildReport<ApiChanges> changesSince(OpenAPI current) {
        BuildReport<OpenAPI> baseline = reports.baseline();
        if (baseline.status() == ReportStatus.UNREADABLE) {
            return BuildReport.unreadable(baseline.file().orElseThrow());
        }
        return baseline.content()
                .map(previous -> BuildReport.read(SwaggerApiChanges.between(previous, current), baseline.file().orElseThrow()))
                .orElse(BuildReport.missing());
    }

    AssessmentContext contextWithoutDocument() {
        return AssessmentContext.empty().withReports(reports.read()).withCoverageTargets(targets);
    }
}
