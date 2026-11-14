// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.AssessmentContext;
import com.brekquater.api.rules.CoverageTargets;
import com.brekquater.api.rules.openapi.SwaggerOpenApiContext;

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
                .map(openApi -> context.withOpenApi(new SwaggerOpenApiContext(openApi)))
                .orElse(context);
    }

    AssessmentContext contextWithoutDocument() {
        return AssessmentContext.empty().withReports(reports.read()).withCoverageTargets(targets);
    }
}
