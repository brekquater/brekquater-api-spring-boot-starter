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
 * BQ-DOC-010: the examples of a parameter, or of a JSON request or response body, are values their schema
 * accepts. {@code format} is not enforced.
 */
public final class ContentExampleMatchesSchema extends OpenApiRule<ApiExample> {

    @Override
    public String id() {
        return "BQ-DOC-010";
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
                .filter(example -> example.origin() == ApiExample.Origin.CONTENT)
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
                "Example does not match its schema: " + String.join("; ", target.problems()) + ".",
                "Make @ExampleObject(value = ...) or @Parameter(example = ...) match what the API accepts or returns."));
    }
}
