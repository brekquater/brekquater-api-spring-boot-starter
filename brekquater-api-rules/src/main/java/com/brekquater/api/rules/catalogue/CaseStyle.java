// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * How a name made of several words joins them. A single lowercase word, such as {@code id}, fits every style and
 * has none. The order is the tie-break when two styles are equally common: camelCase first, Jackson's default.
 */
enum CaseStyle {
    CAMEL_CASE("camelCase", "[a-z][a-z0-9]*([A-Z][a-z0-9]*)+"),
    SNAKE_CASE("snake_case", "[a-z][a-z0-9]*(_[a-z0-9]+)+"),
    KEBAB_CASE("kebab-case", "[a-z][a-z0-9]*(-[a-z0-9]+)+"),
    PASCAL_CASE("PascalCase", "([A-Z][a-z0-9]*)+"),
    MIXED("a mix of styles", "[A-Za-z][A-Za-z0-9_-]*");

    private static final Pattern ONE_WORD = Pattern.compile("[a-z][a-z0-9]*");

    private final String label;
    private final Pattern shape;

    CaseStyle(String label, String shape) {
        this.label = label;
        this.shape = Pattern.compile(shape);
    }

    /** Returns the style of the name, or empty for a single lowercase word or a name that is not an identifier. */
    static Optional<CaseStyle> of(String name) {
        if (ONE_WORD.matcher(name).matches()) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(style -> style.shape.matcher(name).matches()).findFirst();
    }

    String label() {
        return label;
    }
}
