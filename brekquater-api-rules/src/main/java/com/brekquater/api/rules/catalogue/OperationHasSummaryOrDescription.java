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

/** BQ-DOC-001: every operation says what it does, in a summary or a description. */
public final class OperationHasSummaryOrDescription extends OperationRule {

    private static final Explanation UNDOCUMENTED = new Explanation(
            "Operation has no summary or description.",
            "Add @Operation(summary = \"...\") to the controller method.");

    @Override
    public String id() {
        return "BQ-DOC-001";
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
    protected Optional<Explanation> check(ApiOperation operation) {
        boolean documented = hasText(operation.summary()) || hasText(operation.description());
        return documented ? Optional.empty() : Optional.of(UNDOCUMENTED);
    }

    // Whitespace alone tells a reader nothing, so it counts as missing.
    private static boolean hasText(Optional<String> text) {
        return text.filter(Predicate.not(String::isBlank)).isPresent();
    }
}
