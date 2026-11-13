// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.OperationRule;
import com.brekquater.api.rules.Severity;
import java.util.Optional;
import java.util.function.Predicate;

/** BQ-API-001: every operation has an operationId, which client generators use to name methods. */
public final class OperationHasOperationId extends OperationRule {

    private static final Explanation NO_OPERATION_ID = new Explanation(
            "Operation has no operationId.",
            "Set @Operation(operationId = \"...\") on the controller method.");

    @Override
    public String id() {
        return "BQ-API-001";
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
    protected Optional<Explanation> check(ApiOperation operation) {
        boolean identified = operation.operationId().filter(Predicate.not(String::isBlank)).isPresent();
        return identified ? Optional.empty() : Optional.of(NO_OPERATION_ID);
    }
}
