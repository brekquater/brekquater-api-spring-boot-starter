// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * What a schema says about the values it accepts.
 *
 * @param types the declared types; empty when the schema declares none
 * @param format the declared format, if any
 * @param enumValues the allowed values, in document order; empty when the schema is not an enum
 * @param describesValue whether the schema constrains its value at all: a type, a {@code $ref}, properties, items,
 *     an enum, a constant or a composition
 * @param properties what the schema says about its properties
 */
public record ApiSchemaShape(
        Set<String> types,
        Optional<String> format,
        List<ApiLiteral> enumValues,
        boolean describesValue,
        ApiSchemaProperties properties) {

    public ApiSchemaShape {
        types = Set.copyOf(types);
        Objects.requireNonNull(format, "format");
        enumValues = List.copyOf(enumValues);
        Objects.requireNonNull(properties, "properties");
    }
}
