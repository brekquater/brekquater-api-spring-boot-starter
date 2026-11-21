// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiResponse;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.HttpMethod;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * BQ-DES-005: a {@code 200} response has a body. A success with nothing to send is {@code 204 No Content}, which
 * tells clients not to look for one. springdoc documents every {@code void} handler as an empty {@code 200}.
 */
public final class OkResponseHasBody extends OpenApiRule<ApiResponse> {

    private static final String OK = "200";
    // A HEAD response never has a body, and OPTIONS answers with headers, so an empty 200 is right for both.
    private static final Set<HttpMethod> ANSWERED_WITH_HEADERS = EnumSet.of(HttpMethod.HEAD, HttpMethod.OPTIONS);
    private static final Explanation EMPTY_OK = new Explanation(
            "200 response has no body; a success without a body is 204 No Content.",
            "Annotate the method with @ResponseStatus(HttpStatus.NO_CONTENT). springdoc documents the status from "
                    + "that annotation, not from ResponseEntity.noContent().");

    @Override
    public String id() {
        return "BQ-DES-005";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public Category category() {
        return Category.DESIGN;
    }

    @Override
    protected List<ApiResponse> targets(OpenApiContext document) {
        return document.operations().stream()
                .filter(operation -> !ANSWERED_WITH_HEADERS.contains(operation.method()))
                .flatMap(operation -> operation.responses().stream())
                .filter(response -> response.code().equals(OK))
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiResponse response) {
        return response.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiResponse response, OpenApiContext document) {
        return response.content().isEmpty() ? Optional.of(EMPTY_OK) : Optional.empty();
    }
}
