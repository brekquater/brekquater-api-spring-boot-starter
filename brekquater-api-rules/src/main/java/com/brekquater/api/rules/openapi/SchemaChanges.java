// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.ApiChange.Kind;
import com.brekquater.api.rules.ApiLiteral;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.openapi.SchemaPair.Direction;
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
 * away what it promised. A change in a shared schema is recorded once for every operation it affects.
 */
final class SchemaChanges {

    private static final JsonPointer COMPONENT_SCHEMAS = JsonPointer.root().append("components").append("schemas");
    private static final String NULL = "null";
    private static final String INTEGER = "integer";
    private static final String NUMBER = "number";
    // ponytail: a depth cap and a per-walk set of visited ref pairs. Enough for generated documents; a deeper
    // document only loses changes below the cap.
    private static final int MAX_DEPTH = 32;

    private final Flattener oldSchemas;
    private final Flattener newSchemas;
    private final Set<ApiChange> changes = new TreeSet<>();

    SchemaChanges(LocalRefs oldRefs, LocalRefs newRefs) {
        this.oldSchemas = new Flattener(oldRefs);
        this.newSchemas = new Flattener(newRefs);
    }

    void add(ApiChange change) {
        changes.add(change);
    }

    List<ApiChange> changes() {
        return List.copyOf(changes);
    }

    void compare(SchemaPair pair) {
        compare(pair, 0, new HashSet<>());
    }

    private void compare(SchemaPair pair, int depth, Set<String> visited) {
        if (pair.old() == null || pair.now() == null || depth > MAX_DEPTH) {
            return;
        }
        Flat before = oldSchemas.flatten(pair.old(), pair.pointer(), 0);
        Flat after = newSchemas.flatten(pair.now(), pair.pointer(), 0);
        if (visited.add(before.reference() + "|" + after.reference())) {
            compareFlat(before, after, pair, new Walk(depth, visited));
        }
    }

    private void compareFlat(Flat before, Flat after, SchemaPair pair, Walk walk) {
        if (!compatible(before.types(), after.types())) {
            String detail = String.join(", ", before.types()) + " to " + String.join(", ", after.types());
            record(Kind.TYPE_CHANGED, pair.at(after.pointer()), detail);
            return;
        }
        if (pair.direction() == Direction.REQUEST) {
            enumValues(before, after, pair);
            required(before, after, pair);
        } else {
            removed(before, after, pair);
        }
        children(before, after, pair, walk);
    }

    private void children(Flat before, Flat after, SchemaPair pair, Walk walk) {
        after.properties().forEach((name, property) -> Optional.ofNullable(before.properties().get(name)).ifPresent(
                earlier -> compare(pair.child(earlier.schema(), property.schema(), property.pointer())
                        .named("property " + name), walk.depth() + 1, walk.visited())));
        if (before.items() != null && after.items() != null) {
            compare(pair.child(before.items(), after.items(), after.pointer().append("items")), walk.depth() + 1,
                    walk.visited());
        }
    }

    private void enumValues(Flat before, Flat after, SchemaPair pair) {
        if (before.values().isEmpty() || after.values().isEmpty()) {
            return;
        }
        Set<String> lost = new LinkedHashSet<>(before.values());
        lost.removeAll(after.values());
        if (!lost.isEmpty()) {
            record(Kind.REQUEST_VALUE_REMOVED, pair.at(after.pointer()), String.join(", ", lost));
        }
    }

    // A readOnly property is never sent, so requiring it asks nothing of the client.
    private void required(Flat before, Flat after, SchemaPair pair) {
        after.required().forEach(name -> {
            Property property = after.properties().get(name);
            boolean sent = property == null || !Boolean.TRUE.equals(property.schema().getReadOnly());
            if (sent && !(before.required().contains(name) && before.properties().containsKey(name))) {
                JsonPointer pointer = property == null ? after.pointer() : property.pointer();
                record(Kind.PROPERTY_BECAME_REQUIRED, pair.named("property " + name).at(pointer), "");
            }
        });
    }

    // A writeOnly property is never returned, so dropping it takes nothing from the client.
    private void removed(Flat before, Flat after, SchemaPair pair) {
        before.required().forEach(name -> {
            Property property = before.properties().get(name);
            boolean returned = property == null || !Boolean.TRUE.equals(property.schema().getWriteOnly());
            if (returned && !after.properties().containsKey(name)) {
                record(Kind.RESPONSE_PROPERTY_REMOVED, pair.named("property " + name).at(after.pointer()), "");
            }
        });
    }

    private void record(Kind kind, SchemaPair pair, String detail) {
        changes.add(new ApiChange(kind, pair.operation(), pair.pointer(), pair.subject(), detail));
    }

    /** Follows the local {@code $ref}s of one document and merges {@code allOf} branches. */
    private record Flattener(LocalRefs refs) {

        Flat flatten(Schema<?> schema, JsonPointer pointer, int depth) {
            Optional<Schema<?>> target = refs.resolve(schema.get$ref());
            if (target.isPresent() && depth <= MAX_DEPTH) {
                String name = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
                return flatten(target.get(), COMPONENT_SCHEMAS.append(name), depth + 1).referencedAs(schema.get$ref());
            }
            Flat flat = Flat.of(schema, pointer);
            List<?> branches = Optional.<List<?>>ofNullable(schema.getAllOf()).orElse(List.of());
            for (int index = 0; index < branches.size() && depth <= MAX_DEPTH; index++) {
                if (branches.get(index) instanceof Schema<?> branch) {
                    flat = flat.merge(flatten(branch, pointer.append("allOf").append(String.valueOf(index)), depth + 1));
                }
            }
            return flat;
        }
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

    /** How deep one comparison is, and which pairs of schemas it has already compared. */
    private record Walk(int depth, Set<String> visited) {}

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
            Set<String> required = new LinkedHashSet<>(Optional.ofNullable(schema.getRequired()).orElse(List.of()));
            return new Flat(pointer, pointer.value(), types, values(schema), properties(schema, pointer), required,
                    schema.getItems());
        }

        private static Set<String> values(Schema<?> schema) {
            return Optional.<List<?>>ofNullable(schema.getEnum()).orElse(List.of()).stream()
                    .map(JsonValues::literal)
                    .map(ApiLiteral::json)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        private static Map<String, Property> properties(Schema<?> schema, JsonPointer pointer) {
            Map<String, Property> properties = new LinkedHashMap<>();
            Optional.<Map<String, ?>>ofNullable(schema.getProperties()).orElse(Map.of()).forEach((name, property) -> {
                if (property instanceof Schema<?> declared) {
                    properties.put(name, new Property(declared, pointer.append("properties").append(name)));
                }
            });
            return properties;
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
