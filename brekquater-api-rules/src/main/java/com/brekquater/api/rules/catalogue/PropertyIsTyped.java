// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiSchemaNode;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/**
 * BQ-DES-014: a property says what it holds. A schema with no type, {@code $ref}, enum or composition accepts any
 * value, which is what springdoc writes for a field declared as {@code Object}.
 */
public final class PropertyIsTyped extends OpenApiRule<ApiSchemaNode> {

    private static final Explanation UNTYPED = new Explanation(
            "Property has no type, so clients cannot know what it holds.",
            "Declare the field with a specific type instead of Object, or describe it with "
                    + "@Schema(implementation = ...).");

    @Override
    public String id() {
        return "BQ-DES-014";
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
    protected List<ApiSchemaNode> targets(OpenApiContext document) {
        return document.schemas().stream().filter(schema -> schema.propertyName().isPresent()).toList();
    }

    @Override
    protected JsonPointer locate(ApiSchemaNode target) {
        return target.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiSchemaNode target, OpenApiContext document) {
        return target.shape().describesValue() ? Optional.empty() : Optional.of(UNTYPED);
    }
}
