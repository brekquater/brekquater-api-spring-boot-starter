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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * BQ-DES-010: a well-known {@code format} sits on a type it can describe; {@code date-time} on an integer is a
 * contradiction, and code generators pick one side. Unknown and custom formats are ignored. Numeric formats also
 * accept strings, which some APIs use for 64-bit numbers.
 */
public final class FormatFitsType extends OpenApiRule<ApiSchemaNode> {

    private static final Set<String> TEXT = Set.of("string");
    private static final Set<String> WHOLE = Set.of("integer", "string");
    private static final Set<String> DECIMAL = Set.of("number", "string");
    private static final Map<String, Set<String>> EXPECTED = Map.ofEntries(
            Map.entry("date", TEXT),
            Map.entry("date-time", TEXT),
            Map.entry("time", TEXT),
            Map.entry("duration", TEXT),
            Map.entry("uuid", TEXT),
            Map.entry("email", TEXT),
            Map.entry("uri", TEXT),
            Map.entry("hostname", TEXT),
            Map.entry("ipv4", TEXT),
            Map.entry("ipv6", TEXT),
            Map.entry("byte", TEXT),
            Map.entry("binary", TEXT),
            Map.entry("password", TEXT),
            Map.entry("int32", WHOLE),
            Map.entry("int64", WHOLE),
            Map.entry("float", DECIMAL),
            Map.entry("double", DECIMAL));

    @Override
    public String id() {
        return "BQ-DES-010";
    }

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    public Category category() {
        return Category.DESIGN;
    }

    @Override
    protected List<ApiSchemaNode> targets(OpenApiContext document) {
        return document.schemas().stream()
                .filter(schema -> !schema.shape().types().isEmpty())
                .filter(schema -> schema.shape().format().filter(EXPECTED::containsKey).isPresent())
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiSchemaNode target) {
        return target.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiSchemaNode target, OpenApiContext document) {
        String format = target.shape().format().orElseThrow();
        Set<String> types = target.shape().types();
        if (types.stream().anyMatch(EXPECTED.get(format)::contains)) {
            return Optional.empty();
        }
        return Optional.of(new Explanation(
                "Format " + format + " does not fit the type " + String.join(", ", new TreeSet<>(types)) + ".",
                "Give the field the matching Java type, such as OffsetDateTime or UUID, or correct "
                        + "@Schema(type = ..., format = ...)."));
    }
}
