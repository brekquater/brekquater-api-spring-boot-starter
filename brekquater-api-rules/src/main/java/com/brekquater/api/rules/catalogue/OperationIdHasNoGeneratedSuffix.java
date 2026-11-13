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
import java.util.regex.Pattern;

/**
 * BQ-API-002: an operationId does not end in the numeric suffix springdoc adds when two controller methods share a
 * name. Which method gets which suffix can change between builds, so client method names become unstable.
 */
public final class OperationIdHasNoGeneratedSuffix extends OpenApiRule<ApiOperation> {

    private static final Pattern GENERATED_SUFFIX = Pattern.compile(".+_\\d+");

    @Override
    public String id() {
        return "BQ-API-002";
    }

    @Override
    public Severity severity() {
        return Severity.INFO;
    }

    @Override
    public Category category() {
        return Category.DESIGN;
    }

    @Override
    protected List<ApiOperation> targets(OpenApiContext document) {
        return DocumentTargets.operationsWithId(document);
    }

    @Override
    protected JsonPointer locate(ApiOperation operation) {
        return operation.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiOperation operation, OpenApiContext document) {
        String operationId = operation.operationId().orElseThrow();
        return GENERATED_SUFFIX.matcher(operationId).matches() ? Optional.of(suffixed(operationId)) : Optional.empty();
    }

    private static Explanation suffixed(String operationId) {
        return new Explanation(
                "The operationId \"" + operationId + "\" looks generated to tell apart methods with the same name.",
                "Rename one of the controller methods, or set @Operation(operationId = \"...\") explicitly.");
    }
}
