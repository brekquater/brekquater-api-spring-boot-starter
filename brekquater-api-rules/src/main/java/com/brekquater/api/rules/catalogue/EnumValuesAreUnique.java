// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiLiteral;
import com.brekquater.api.rules.ApiSchemaNode;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BQ-DES-012: an {@code enum} lists each value once. Code generators turn a repeat into two constants with one
 * name, and it usually means two Java constants serialize to the same text.
 */
public final class EnumValuesAreUnique extends OpenApiRule<ApiSchemaNode> {

    @Override
    public String id() {
        return "BQ-DES-012";
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
        return document.schemas().stream().filter(schema -> !schema.shape().enumValues().isEmpty()).toList();
    }

    @Override
    protected JsonPointer locate(ApiSchemaNode target) {
        return target.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiSchemaNode target, OpenApiContext document) {
        Set<ApiLiteral> seen = new HashSet<>();
        String repeated = target.shape().enumValues().stream()
                .filter(value -> !seen.add(value))
                .map(ApiLiteral::json)
                .distinct()
                .collect(Collectors.joining(", "));
        if (repeated.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Explanation(
                "Enum lists these values more than once: " + repeated + ".",
                "Give each constant its own value; check @JsonValue, @JsonProperty and toString() on the enum."));
    }
}
