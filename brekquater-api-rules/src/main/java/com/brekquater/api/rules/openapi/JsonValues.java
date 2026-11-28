// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiLiteral;
import java.util.Locale;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Turns the loosely typed values swagger-models holds into JSON trees. */
final class JsonValues {

    static final JsonMapper MAPPER = JsonMapper.shared();

    private static final String JACKSON_2 = "com.fasterxml.jackson.databind.";

    private JsonValues() {}

    // swagger-core reads documents with Jackson 2, so a value may be a Jackson 2 node, whose text is its JSON.
    static JsonNode tree(Object value) {
        if (value != null && value.getClass().getName().startsWith(JACKSON_2)) {
            return MAPPER.readTree(value.toString());
        }
        return MAPPER.valueToTree(value);
    }

    static ApiLiteral literal(Object value) {
        JsonNode node = tree(value);
        return new ApiLiteral(node.toString(), typeOf(node));
    }

    private static String typeOf(JsonNode node) {
        if (node.isNumber()) {
            return node.canConvertToExactIntegral() ? "integer" : "number";
        }
        return node.getNodeType().name().toLowerCase(Locale.ROOT);
    }
}
