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
 * BQ-NAM-006: no two properties of one object differ only in case, such as {@code url} and {@code URL}.
 * Languages and generators that ignore or change case turn the two into one name.
 */
public final class PropertyNamesDifferBeyondCase extends OpenApiRule<ApiSchemaNode> {

    @Override
    public String id() {
        return "BQ-NAM-006";
    }

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    public Category category() {
        return Category.NAMING;
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
        String name = target.propertyName().orElseThrow();
        return document.schemas().stream()
                .filter(other -> other.propertyName().isPresent() && owner(other).equals(owner(target)))
                .map(other -> other.propertyName().orElseThrow())
                .filter(other -> !other.equals(name) && other.equalsIgnoreCase(name))
                .findFirst()
                .map(other -> new Explanation(
                        "Property " + name + " differs only in case from " + other + " in the same object.",
                        "Rename one of them, in the field or with @JsonProperty."));
    }

    private static List<String> owner(ApiSchemaNode property) {
        List<String> tokens = property.pointer().tokens();
        return tokens.subList(0, tokens.size() - 1);
    }
}
