// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiMediaType;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/** BQ-API-003: every documented response body says what it contains, through a schema. */
public final class ResponseContentHasSchema extends OpenApiRule<ApiMediaType> {

    private static final Explanation NO_SCHEMA = new Explanation(
            "Response body has no schema.",
            "Return a concrete type from the controller method instead of ResponseEntity<?>, or describe the body "
                    + "with @ApiResponse(content = @Content(schema = @Schema(implementation = ...))).");

    @Override
    public String id() {
        return "BQ-API-003";
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
    protected List<ApiMediaType> targets(OpenApiContext document) {
        return DocumentTargets.responseMediaTypes(document);
    }

    @Override
    protected JsonPointer locate(ApiMediaType mediaType) {
        return mediaType.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiMediaType mediaType, OpenApiContext document) {
        return mediaType.schema().isPresent() ? Optional.empty() : Optional.of(NO_SCHEMA);
    }
}
