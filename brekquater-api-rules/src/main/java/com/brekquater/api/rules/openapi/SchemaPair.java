// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Two versions of one schema to compare, and where they sit.
 *
 * @param old the schema in the baseline
 * @param now the schema in the current document
 * @param pointer where the current schema is
 * @param subject what the schema describes, such as {@code request body} or {@code property total}
 * @param direction whether clients send the value or receive it
 * @param operation the operation the schema was reached from
 */
record SchemaPair(
        Schema<?> old, Schema<?> now, JsonPointer pointer, String subject, Direction direction, JsonPointer operation) {

    /** Whether clients send a value or receive it, which decides what counts as breaking them. */
    enum Direction {
        REQUEST,
        RESPONSE
    }

    SchemaPair child(Schema<?> oldChild, Schema<?> newChild, JsonPointer childPointer) {
        return new SchemaPair(oldChild, newChild, childPointer, subject, direction, operation);
    }

    SchemaPair at(JsonPointer newPointer) {
        return new SchemaPair(old, now, newPointer, subject, direction, operation);
    }

    SchemaPair named(String newSubject) {
        return new SchemaPair(old, now, pointer, newSubject, direction, operation);
    }
}
