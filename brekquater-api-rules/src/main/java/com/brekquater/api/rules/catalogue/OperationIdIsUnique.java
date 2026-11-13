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

/** BQ-API-004: no two operations share an operationId, because generated clients would get clashing methods. */
public final class OperationIdIsUnique extends OpenApiRule<ApiOperation> {

    @Override
    public String id() {
        return "BQ-API-004";
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
        long sharing = targets(document).stream()
                .filter(other -> other.operationId().orElseThrow().equals(operationId))
                .count();
        return sharing > 1 ? Optional.of(duplicated(operationId)) : Optional.empty();
    }

    private static Explanation duplicated(String operationId) {
        return new Explanation(
                "The operationId \"" + operationId + "\" is used by more than one operation.",
                "Give each operation its own @Operation(operationId = \"...\").");
    }
}
