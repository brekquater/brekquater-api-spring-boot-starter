// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiExample;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/**
 * BQ-DOC-009: a schema's {@code example}, {@code examples} and {@code default} are values the schema accepts. An
 * example is the first thing a client developer copies. {@code format} is not enforced.
 */
public final class SchemaExampleMatchesSchema extends OpenApiRule<ApiExample> {

    @Override
    public String id() {
        return "BQ-DOC-009";
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
    protected List<ApiExample> targets(OpenApiContext document) {
        return document.examples().stream()
                .filter(example -> example.origin() == ApiExample.Origin.SCHEMA)
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiExample target) {
        return target.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiExample target, OpenApiContext document) {
        if (target.problems().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Explanation(
                "Example or default does not match its schema: " + String.join("; ", target.problems()) + ".",
                "Correct @Schema(example = ...) or the default value, or the field's type and constraints."));
    }
}
