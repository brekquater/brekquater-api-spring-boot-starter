// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.ApiChange.Kind;
import com.brekquater.api.rules.ApiLiteral;
import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.media.Schema;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Compares two versions of a schema, following local {@code $ref}s and merging {@code allOf}, and collects the
 * changes a client would notice. A request schema may not demand more than before; a response schema may not take
 * away what it promised. A change found through a shared schema is reported once, where that schema is defined.
 */
final class SchemaChanges {

    private static final JsonPointer COMPONENT_SCHEMAS = JsonPointer.root().append("components").append("schemas");
    private static final String NULL = "null";
    private static final String INTEGER = "integer";
    private static final String NUMBER = "number";
    // ponytail: a depth cap and a per-walk set of visited ref pairs. Enough for generated documents; a deeper
    // document only loses changes below the cap.
    private static final int MAX_DEPTH = 32;

    private final LocalRefs oldRefs;
    private final LocalRefs newRefs;
    private final Set<ApiChange> changes = new TreeSet<>();

    SchemaChanges(LocalRefs oldRefs, LocalRefs newRefs) {
        this.oldRefs = oldRefs;
        this.newRefs = newRefs;
    }

    void add(Kind kind, JsonPointer pointer, String subject, String detail) {
        changes.add(new ApiChange(kind, pointer, subject, detail));
    }

    List<ApiChange> changes() {
        return List.copyOf(changes);
    }

    void compareRequest(Schema<?> old, Schema<?> now, JsonPointer pointer, String subject) {
        compare(new Pair(old, now, pointer, subject, true), 0, new HashSet<>());
    }

    void compareResponse(Schema<?> old, Schema<?> now, JsonPointer pointer, String subject) {
        compare(new Pair(old, now, pointer, subject, false), 0, new HashSet<>());
    }

    private void compare(Pair pair, int depth, Set<String> visited) {
        if (pair.old() == null || pair.now() == null || depth > MAX_DEPTH) {
            return;
        }
        Flat before = flatten(pair.old(), pair.pointer(), oldRefs, 0);
        Flat after = flatten(pair.now(), pair.pointer(), newRefs, 0);
        if (!visited.add(before.reference() + "|" + after.reference())) {
            return;
        }
        if (!compatible(before.types(), after.types())) {
            add(Kind.TYPE_CHANGED, after.pointer(), pair.subject(), String.join(", ", before.types()) + " to "
                    + String.join(", ", after.types()));
            return;
        }
        if (pair.request()) {
            enumValues(before, after, pair.subject());
            required(before, after);
        } else {
            removed(before, after);
        }
        children(before, after, pair, depth, visited);
    }

    private void children(Flat before, Flat after, Pair pair, int depth, Set<String> visited) {
        after.properties().forEach((name, property) -> Optional.ofNullable(before.properties().get(name)).ifPresent(
                earlier -> compare(new Pair(earlier.schema(), property.schema(), property.pointer(), "property " + name,
                        pair.request()), depth + 1, visited)));
        if (before.items() != null && after.items() != null) {
            compare(new Pair(before.items(), after.items(), after.pointer().append("items"), pair.subject(),
                    pair.request()), depth + 1, visited);
        }
    }

    private void enumValues(Flat before, Flat after, String subject) {
        if (before.values().isEmpty() || after.values().isEmpty()) {
            return;
        }
        Set<String> lost = new LinkedHashSet<>(before.values());
        lost.removeAll(after.values());
        if (!lost.isEmpty()) {
            add(Kind.REQUEST_VALUE_REMOVED, after.pointer(), subject, String.join(", ", lost));
        }
    }

    // A readOnly property is never sent, so requiring it asks nothing of the client.
    private void required(Flat before, Flat after) {
        after.required().forEach(name -> {
            Property property = after.properties().get(name);
            boolean sent = property == null || !Boolean.TRUE.equals(property.schema().getReadOnly());
            if (sent && !(before.required().contains(name) && before.properties().containsKey(name))) {
                JsonPointer pointer = property == null ? after.pointer() : property.pointer();
                add(Kind.PROPERTY_BECAME_REQUIRED, pointer, "property " + name, "");
            }
        });
    }

    // A writeOnly property is never returned, so dropping it takes nothing from the client.
    private void removed(Flat before, Flat after) {
        before.required().forEach(name -> {
            Property property = before.properties().get(name);
            boolean returned = property == null || !Boolean.TRUE.equals(property.schema().getWriteOnly());
            if (returned && !after.properties().containsKey(name)) {
                add(Kind.RESPONSE_PROPERTY_REMOVED, after.pointer(), "property " + name, "");
            }
        });
    }

    private Flat flatten(Schema<?> schema, JsonPointer pointer, LocalRefs refs, int depth) {
        Optional<Schema<?>> target = refs.resolve(schema.get$ref());
        if (target.isPresent() && depth <= MAX_DEPTH) {
            String name = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
            return flatten(target.get(), COMPONENT_SCHEMAS.append(name), refs, depth + 1).referencedAs(schema.get$ref());
        }
        Flat flat = Flat.of(schema, pointer);
        List<?> branches = Optional.<List<?>>ofNullable(schema.getAllOf()).orElse(List.of());
        for (int index = 0; index < branches.size() && depth <= MAX_DEPTH; index++) {
            if (branches.get(index) instanceof Schema<?> branch) {
                flat = flat.merge(flatten(branch, pointer.append("allOf").append(String.valueOf(index)), refs, depth + 1));
            }
        }
        return flat;
    }

    // Integers are numbers, so integer and number may replace each other; an undeclared type matches anything.
    private static boolean compatible(Set<String> before, Set<String> after) {
        if (before.isEmpty() || after.isEmpty()) {
            return true;
        }
        return before.stream().anyMatch(type -> after.contains(type)
                || type.equals(INTEGER) && after.contains(NUMBER)
                || type.equals(NUMBER) && after.contains(INTEGER));
    }

    private record Pair(Schema<?> old, Schema<?> now, JsonPointer pointer, String subject, boolean request) {}

    private record Property(Schema<?> schema, JsonPointer pointer) {}

    /** A schema with its {@code allOf} branches merged into one. */
    private record Flat(
            JsonPointer pointer,
            String reference,
            Set<String> types,
            Set<String> values,
            Map<String, Property> properties,
            Set<String> required,
            Schema<?> items) {

        static Flat of(Schema<?> schema, JsonPointer pointer) {
            Set<String> types = new TreeSet<>(SchemaFacts.types(schema));
            types.remove(NULL);
            Set<String> values = Optional.<List<?>>ofNullable(schema.getEnum()).orElse(List.of()).stream()
                    .map(JsonValues::literal)
                    .map(ApiLiteral::json)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Map<String, Property> properties = new LinkedHashMap<>();
            Optional.<Map<String, ?>>ofNullable(schema.getProperties()).orElse(Map.of()).forEach((name, property) -> {
                if (property instanceof Schema<?> declared) {
                    properties.put(name, new Property(declared, pointer.append("properties").append(name)));
                }
            });
            Set<String> required = new LinkedHashSet<>(Optional.ofNullable(schema.getRequired()).orElse(List.of()));
            return new Flat(pointer, pointer.value(), types, values, properties, required, schema.getItems());
        }

        Flat referencedAs(String ref) {
            return new Flat(pointer, ref, types, values, properties, required, items);
        }

        Flat merge(Flat branch) {
            Set<String> mergedTypes = new TreeSet<>(types);
            mergedTypes.addAll(branch.types());
            Map<String, Property> mergedProperties = new LinkedHashMap<>(branch.properties());
            mergedProperties.putAll(properties);
            Set<String> mergedRequired = new LinkedHashSet<>(required);
            mergedRequired.addAll(branch.required());
            return new Flat(pointer, reference, mergedTypes, values, mergedProperties, mergedRequired,
                    items != null ? items : branch.items());
        }
    }
}
