// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.Finding;
import com.brekquater.api.rules.Severity;
import com.brekquater.api.rules.report.Assessment;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * Fails startup when the assessment has blocker findings, for teams that opt in with
 * {@code brekquater.api.strict=true}.
 *
 * <p>It runs once every singleton exists, so springdoc can already build the document, and before the web server
 * accepts connections, so a failing application never serves a request. The same check therefore fails a
 * {@code @SpringBootTest} in CI.
 */
final class StrictModeCheck implements SmartInitializingSingleton {

    private static final Log LOGGER = LogFactory.getLog(StrictModeCheck.class);

    private final OpenApiDocumentSource source;
    private final AssessmentService service;
    private final String contextPath;

    StrictModeCheck(OpenApiDocumentSource source, AssessmentService service, String contextPath) {
        this.source = source;
        this.service = service;
        this.contextPath = contextPath;
    }

    @Override
    public void afterSingletonsInstantiated() {
        Assessment assessment = service.assess(source.fetch(StartupDocumentRequest.create(contextPath)));
        if (assessment.result().document().isEmpty()) {
            LOGGER.warn("Strict mode found no OpenAPI document, so only the build reports were checked.");
        }
        List<Finding> blockers = assessment.result().findings().stream()
                .filter(finding -> finding.severity() == Severity.BLOCKER)
                .toList();
        if (!blockers.isEmpty()) {
            throw new StrictModeViolationException(blockers);
        }
        LOGGER.info("Strict mode found no blocker findings.");
    }
}
