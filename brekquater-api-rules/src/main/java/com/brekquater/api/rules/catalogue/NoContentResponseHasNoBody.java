// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiResponse;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * BQ-DES-007: a {@code 204}, {@code 205} or {@code 304} response documents no body. HTTP forbids a body on these
 * codes, so a documented one either never arrives or means the status code is wrong.
 */
public final class NoContentResponseHasNoBody extends OpenApiRule<ApiResponse> {

    // RFC 9110 sections 15.3.5, 15.3.6 and 15.4.5.
    private static final Set<String> WITHOUT_CONTENT = Set.of("204", "205", "304");
    private static final Explanation BODY_ON_NO_CONTENT = new Explanation(
            "Response documents a body, but HTTP sends no body with this status code.",
            "Return void or ResponseEntity<Void> from the method, or keep the body and answer 200 instead of "
                    + "@ResponseStatus(HttpStatus.NO_CONTENT).");

    @Override
    public String id() {
        return "BQ-DES-007";
    }

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    public Category category() {
        return Category.DESIGN;
    }

    @Override
    protected List<ApiResponse> targets(OpenApiContext document) {
        return DocumentTargets.responses(document).stream()
                .filter(response -> WITHOUT_CONTENT.contains(response.code()))
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiResponse response) {
        return response.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiResponse response, OpenApiContext document) {
        return response.content().isEmpty() ? Optional.empty() : Optional.of(BODY_ON_NO_CONTENT);
    }
}
