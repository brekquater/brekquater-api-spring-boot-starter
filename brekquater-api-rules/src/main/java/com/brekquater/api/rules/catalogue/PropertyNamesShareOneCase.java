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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * BQ-NAM-005: property names follow one case style, whichever most of the API's names already use. Guidelines
 * disagree on the style (Zalando asks for snake_case, Microsoft and Google for camelCase), but all of them ask for
 * one, because a client cannot guess {@code customerName} next to {@code order_id}.
 */
public final class PropertyNamesShareOneCase extends OpenApiRule<PropertyNamesShareOneCase.NamedProperty> {

    @Override
    public String id() {
        return "BQ-NAM-005";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public Category category() {
        return Category.NAMING;
    }

    @Override
    protected List<NamedProperty> targets(OpenApiContext document) {
        List<ApiSchemaNode> styled = document.schemas().stream()
                .filter(schema -> schema.propertyName().flatMap(CaseStyle::of).isPresent())
                .toList();
        CaseStyle common = mostCommon(styled.stream().map(PropertyNamesShareOneCase::styleOf).toList());
        return styled.stream()
                .map(schema -> new NamedProperty(schema.pointer(), schema.propertyName().orElseThrow(), styleOf(schema), common))
                .toList();
    }

    @Override
    protected JsonPointer locate(NamedProperty property) {
        return property.pointer();
    }

    @Override
    protected Optional<Explanation> check(NamedProperty property, OpenApiContext document) {
        if (property.style() == property.common()) {
            return Optional.empty();
        }
        return Optional.of(new Explanation(
                "Property " + property.name() + " is " + property.style().label() + ", while most property names "
                        + "in this API are " + property.common().label() + ".",
                "Rename it with @JsonProperty, or set one naming strategy for the class with @JsonNaming."));
    }

    private static CaseStyle styleOf(ApiSchemaNode property) {
        return property.propertyName().flatMap(CaseStyle::of).orElseThrow();
    }

    // A mix of styles inside one name is never the convention to follow.
    private static CaseStyle mostCommon(List<CaseStyle> styles) {
        Map<CaseStyle, Integer> counts = new EnumMap<>(CaseStyle.class);
        styles.stream()
                .filter(style -> style != CaseStyle.MIXED)
                .forEach(style -> counts.merge(style, 1, Integer::sum));
        return counts.entrySet().stream()
                .max(Comparator.comparing(Map.Entry<CaseStyle, Integer>::getValue)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse(CaseStyle.CAMEL_CASE);
    }

    /** One property name, its style, and the style most of the API's property names use. */
    record NamedProperty(JsonPointer pointer, String name, CaseStyle style, CaseStyle common) {}
}
