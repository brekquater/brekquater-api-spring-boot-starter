// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Comparator;
import java.util.Objects;

/**
 * One difference between the OpenAPI document of the last release and the current one that can affect clients.
 *
 * @param kind what changed
 * @param pointer where the change is: in the current document, or in the baseline for a removed operation
 * @param subject what changed, such as {@code GET /orders}, {@code query parameter page} or {@code property total}
 * @param detail how it changed, such as {@code integer to string}; empty when the kind says it all
 */
public record ApiChange(Kind kind, JsonPointer pointer, String subject, String detail) implements Comparable<ApiChange> {

    private static final Comparator<ApiChange> ORDER = Comparator.comparing((ApiChange change) -> change.pointer().value())
            .thenComparing(ApiChange::kind)
            .thenComparing(ApiChange::subject)
            .thenComparing(ApiChange::detail);

    /** What changed. Every kind except {@link #SUNSET_MOVED_EARLIER} can break a client that worked before. */
    public enum Kind {
        /** An operation was removed without first being deprecated. */
        OPERATION_REMOVED,
        /** A parameter became required, or a new required parameter was added. */
        PARAMETER_BECAME_REQUIRED,
        /** The request body became required. */
        REQUEST_BODY_BECAME_REQUIRED,
        /** A request property became required, or a new required request property was added. */
        PROPERTY_BECAME_REQUIRED,
        /** A property that every response had is gone. */
        RESPONSE_PROPERTY_REMOVED,
        /** A value clients could send is no longer allowed. */
        REQUEST_VALUE_REMOVED,
        /** A success status code is no longer documented. */
        SUCCESS_STATUS_REMOVED,
        /** A parameter, body or property changed to an incompatible type. */
        TYPE_CHANGED,
        /** An {@code x-sunset} date moved earlier than the baseline promised. */
        SUNSET_MOVED_EARLIER;

        public boolean breaking() {
            return this != SUNSET_MOVED_EARLIER;
        }
    }

    public ApiChange {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(pointer, "pointer");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(detail, "detail");
    }

    @Override
    public int compareTo(ApiChange other) {
        return ORDER.compare(this, other);
    }
}
