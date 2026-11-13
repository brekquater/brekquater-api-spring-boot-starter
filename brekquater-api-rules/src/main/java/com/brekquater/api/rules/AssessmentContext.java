// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;
import java.util.Optional;

/**
 * Everything the rules may read during one assessment. An input that could not be obtained is empty, never null.
 *
 * @param openApi the application's OpenAPI document
 */
public record AssessmentContext(Optional<OpenApiContext> openApi) {

    private static final AssessmentContext EMPTY = new AssessmentContext(Optional.empty());

    public AssessmentContext {
        Objects.requireNonNull(openApi, "openApi");
    }

    public static AssessmentContext of(OpenApiContext openApi) {
        return new AssessmentContext(Optional.of(openApi));
    }

    /** Returns a context with no inputs, for example when the application has no OpenAPI document. */
    public static AssessmentContext empty() {
        return EMPTY;
    }

    public boolean provides(RuleInput input) {
        return switch (input) {
            case OPENAPI -> openApi.isPresent();
        };
    }
}
