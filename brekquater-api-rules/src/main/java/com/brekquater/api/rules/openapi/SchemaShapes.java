// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiLiteral;
import com.brekquater.api.rules.ApiSchemaProperties;
import com.brekquater.api.rules.ApiSchemaShape;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Turns a swagger-models schema into an {@link ApiSchemaShape}. */
final class SchemaShapes {

    private final LocalRefs refs;

    SchemaShapes(LocalRefs refs) {
        this.refs = refs;
    }

    ApiSchemaShape of(Schema<?> schema) {
        Set<String> types = SchemaFacts.types(schema);
        return new ApiSchemaShape(
                types,
                Optional.ofNullable(schema.getFormat()),
                literals(schema),
                describesValue(schema, types),
                properties(schema));
    }

    private static List<ApiLiteral> literals(Schema<?> schema) {
        List<?> values = schema.getEnum() == null ? List.of() : schema.getEnum();
        return values.stream().map(JsonValues::literal).toList();
    }

    private static boolean describesValue(Schema<?> schema, Set<String> types) {
        return !types.isEmpty()
                || schema.get$ref() != null
                || schema.getProperties() != null
                || schema.getItems() != null
                || schema.getAdditionalProperties() != null
                || schema.getEnum() != null
                || schema.getConst() != null
                || schema.getNot() != null
                || schema.getAllOf() != null
                || schema.getAnyOf() != null
                || schema.getOneOf() != null;
    }

    private ApiSchemaProperties properties(Schema<?> schema) {
        Merged merged = merge(schema, new HashSet<>());
        return new ApiSchemaProperties(
                merged.defined(),
                merged.required(),
                merged.complete(),
                Optional.ofNullable(schema.getDiscriminator()).map(discriminator -> discriminator.getPropertyName()));
    }

    // "visited" holds the refs on the current branch, so schemas that refer to each other end the walk.
    private Merged merge(Object candidate, Set<String> visited) {
        if (!(candidate instanceof Schema<?> schema)) {
            return Merged.nothing(true);
        }
        if (schema.get$ref() != null) {
            return mergeReference(schema.get$ref(), visited);
        }
        Set<String> defined = new HashSet<>(ownProperties(schema));
        Set<String> demanded = new HashSet<>(required(schema));
        List<Merged> all = branches(schema.getAllOf(), visited);
        all.forEach(branch -> demanded.addAll(branch.required()));
        List<Merged> either = branches(schema.getAnyOf(), visited);
        either.addAll(branches(schema.getOneOf(), visited));
        demanded.addAll(inEvery(either));
        all.addAll(either);
        all.forEach(branch -> defined.addAll(branch.defined()));
        return new Merged(defined, demanded, all.stream().allMatch(Merged::complete));
    }

    private Merged mergeReference(String reference, Set<String> visited) {
        Optional<Schema<?>> target = refs.resolve(reference);
        if (target.isEmpty() || visited.contains(reference)) {
            return Merged.nothing(target.isPresent());
        }
        Set<String> deeper = new HashSet<>(visited);
        deeper.add(reference);
        return merge(target.get(), deeper);
    }

    private List<Merged> branches(List<?> schemas, Set<String> visited) {
        List<?> present = schemas == null ? List.of() : schemas;
        return new ArrayList<>(present.stream().map(branch -> merge(branch, visited)).toList());
    }

    private static Set<String> inEvery(List<Merged> branches) {
        if (branches.isEmpty()) {
            return Set.of();
        }
        Set<String> shared = new HashSet<>(branches.get(0).required());
        branches.forEach(branch -> shared.retainAll(branch.required()));
        return shared;
    }

    private static List<String> ownProperties(Schema<?> schema) {
        Map<?, ?> properties = schema.getProperties() == null ? Map.of() : schema.getProperties();
        return properties.keySet().stream().map(String::valueOf).toList();
    }

    private static Set<String> required(Schema<?> schema) {
        return schema.getRequired() == null ? Set.of() : new HashSet<>(schema.getRequired());
    }

    private record Merged(Set<String> defined, Set<String> required, boolean complete) {

        static Merged nothing(boolean complete) {
            return new Merged(Set.of(), Set.of(), complete);
        }
    }
}
