// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.report.Assessment;
import java.util.Optional;

/** Assesses an OpenAPI document. Declare a bean of this type to replace how assessments are made. */
public interface AssessmentService {

    /**
     * Returns the assessment of the document. An empty or unreadable document gives an assessment that says the
     * API was not assessed; it never throws for one.
     */
    Assessment assess(Optional<OpenApiDocument> document);
}
