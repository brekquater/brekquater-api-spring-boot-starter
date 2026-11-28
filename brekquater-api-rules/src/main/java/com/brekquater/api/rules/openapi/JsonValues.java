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

    private JsonValues() {}

    static JsonNode tree(Object value) {
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
