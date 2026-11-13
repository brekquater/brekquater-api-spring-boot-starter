// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Comparator;
import java.util.Objects;

/**
 * Where a finding was observed.
 *
 * @param source the artefact the location refers to
 * @param location the place inside that artefact; a JSON Pointer for {@link RuleInput#OPENAPI}
 */
public record Evidence(RuleInput source, String location) implements Comparable<Evidence> {

    private static final Comparator<Evidence> ORDER =
            Comparator.comparing(Evidence::source).thenComparing(Evidence::location);

    public Evidence {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(location, "location");
    }

    /** Returns evidence that points into the OpenAPI document. */
    public static Evidence at(JsonPointer pointer) {
        return new Evidence(RuleInput.OPENAPI, pointer.value());
    }

    @Override
    public int compareTo(Evidence other) {
        return ORDER.compare(this, other);
    }
}
