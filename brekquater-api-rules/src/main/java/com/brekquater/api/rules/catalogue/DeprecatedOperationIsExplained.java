// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/**
 * BQ-DOC-012: a deprecated operation has a description, where clients learn what replaces it and until when it
 * works. {@code deprecated: true} alone says only that something will change.
 */
public final class DeprecatedOperationIsExplained extends OpenApiRule<ApiOperation> {

    private static final Explanation UNEXPLAINED = new Explanation(
            "Deprecated operation does not say what replaces it.",
            "Describe the replacement and the end date, for example @Operation(deprecated = true, description = "
                    + "\"Replaced by GET /v2/orders. Works until 2027-06-30.\").");

    @Override
    public String id() {
        return "BQ-DOC-012";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public Category category() {
        return Category.DOCUMENTATION;
    }

    @Override
    protected List<ApiOperation> targets(OpenApiContext document) {
        return document.operations().stream().filter(ApiOperation::deprecated).toList();
    }

    @Override
    protected JsonPointer locate(ApiOperation target) {
        return target.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiOperation target, OpenApiContext document) {
        return target.description().filter(text -> !text.isBlank()).isPresent() ? Optional.empty() : Optional.of(UNEXPLAINED);
    }
}
