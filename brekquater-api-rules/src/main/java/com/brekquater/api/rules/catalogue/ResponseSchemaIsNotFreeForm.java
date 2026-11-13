// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiMediaType;
import com.brekquater.api.rules.ApiSchema;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/**
 * BQ-API-005: a response schema describes its fields instead of allowing any object. springdoc writes such a
 * schema for {@code Object}, {@code Map<String, Object>} and {@code ResponseEntity<?>} return types.
 */
public final class ResponseSchemaIsNotFreeForm extends OpenApiRule<ApiMediaType> {

    private static final Explanation FREE_FORM = new Explanation(
            "Response schema allows any object, so clients cannot know its fields.",
            "Return a record or class with named fields instead of Object, Map<String, Object> or ResponseEntity<?>.");

    @Override
    public String id() {
        return "BQ-API-005";
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
    protected List<ApiMediaType> targets(OpenApiContext document) {
        return DocumentTargets.responseMediaTypes(document).stream()
                .filter(mediaType -> mediaType.schema().isPresent())
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiMediaType mediaType) {
        return mediaType.pointer().append("schema");
    }

    @Override
    protected Optional<Explanation> check(ApiMediaType mediaType, OpenApiContext document) {
        boolean freeForm = mediaType.schema().filter(ApiSchema::isFreeFormObject).isPresent();
        return freeForm ? Optional.of(FREE_FORM) : Optional.empty();
    }
}
