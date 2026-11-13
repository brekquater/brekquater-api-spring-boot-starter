// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.ApiResponse;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.OperationRule;
import com.brekquater.api.rules.Severity;
import java.util.Optional;

/**
 * BQ-ERR-001: every operation documents at least one error response. Any client or server error counts; the rule
 * does not demand a particular status code.
 */
public final class OperationDocumentsErrorResponse extends OperationRule {

    private static final Explanation NO_ERROR_RESPONSE = new Explanation(
            "Operation documents no error response.",
            "Add @ApiResponse(responseCode = \"400\", ...) to the controller method, or handle the error in a "
                    + "@ControllerAdvice method annotated with @ResponseStatus.");

    @Override
    public String id() {
        return "BQ-ERR-001";
    }

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    public Category category() {
        return Category.ERRORS;
    }

    @Override
    protected Optional<Explanation> check(ApiOperation operation) {
        boolean documentsAnError = operation.responses().stream().anyMatch(ApiResponse::isError);
        return documentsAnError ? Optional.empty() : Optional.of(NO_ERROR_RESPONSE);
    }
}
