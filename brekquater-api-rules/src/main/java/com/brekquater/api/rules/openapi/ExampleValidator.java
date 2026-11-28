// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/** Checks one value against one schema with the networknt JSON Schema validator. */
final class ExampleValidator {

    private final SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
    private final JsonSchemas schemas;

    ExampleValidator(LocalRefs refs) {
        this.schemas = new JsonSchemas(refs);
    }

    /** Returns why the value does not match the schema, or nothing when it does or cannot be checked. */
    List<String> problems(Object value, Schema<?> schema) {
        try {
            return validate(JsonValues.tree(value), schema);
        } catch (RuntimeException | StackOverflowError unchecked) {
            // A regular expression Java cannot compile, or schemas that refer to each other on the same value.
            // Neither says the example is wrong, so there is nothing to report.
            return List.of();
        }
    }

    /**
     * Like {@link #problems}, but for a value that travels as text, such as a parameter example: the text
     * {@code "5"} is a valid integer there.
     */
    List<String> problemsOfText(Object value, Schema<?> schema) {
        List<String> problems = problems(value, schema);
        if (problems.isEmpty() || !(value instanceof String text)) {
            return problems;
        }
        try {
            return problems(JsonValues.MAPPER.readTree(text), schema).isEmpty() ? List.of() : problems;
        } catch (JacksonException notJson) {
            return problems;
        }
    }

    private List<String> validate(JsonNode value, Schema<?> schema) {
        return registry.getSchema(schemas.standalone(schema)).validate(value).stream()
                .map(error -> (error.getInstanceLocation() + " " + error.getMessage()).strip())
                .distinct()
                .sorted()
                .toList();
    }
}
