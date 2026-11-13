// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiMediaType;
import com.brekquater.api.rules.ApiResponse;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/**
 * BQ-ERR-002: an error response with a body is sent as RFC 9457 problem details, so every client can read every
 * error the same way.
 */
public final class ErrorResponseUsesProblemDetails extends OpenApiRule<ApiResponse> {

    private static final Explanation NOT_PROBLEM_DETAILS = new Explanation(
            "Error response is not documented as application/problem+json.",
            "Return ProblemDetail from the @ExceptionHandler method and declare "
                    + "@ApiResponse(content = @Content(mediaType = \"application/problem+json\")).");

    @Override
    public String id() {
        return "BQ-ERR-002";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public Category category() {
        return Category.ERRORS;
    }

    @Override
    protected List<ApiResponse> targets(OpenApiContext document) {
        return DocumentTargets.responses(document).stream()
                .filter(ApiResponse::isError)
                .filter(response -> !response.content().isEmpty())
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiResponse response) {
        return response.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiResponse response, OpenApiContext document) {
        boolean problemDetails = response.content().stream().anyMatch(ApiMediaType::isProblemDetails);
        return problemDetails ? Optional.empty() : Optional.of(NOT_PROBLEM_DETAILS);
    }
}
