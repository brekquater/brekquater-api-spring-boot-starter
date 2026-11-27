// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * How the current OpenAPI document differs from the one of the last release, the baseline.
 *
 * @param changes the differences that can affect clients, in a fixed order
 * @param comparedOperations how many operations the baseline has, each one checked against the current document
 * @param baselineVersion the baseline's {@code info.version}
 * @param currentVersion the current document's {@code info.version}
 */
public record ApiChanges(
        List<ApiChange> changes, int comparedOperations, Optional<String> baselineVersion, Optional<String> currentVersion) {

    public ApiChanges {
        changes = List.copyOf(changes);
        Objects.requireNonNull(baselineVersion, "baselineVersion");
        Objects.requireNonNull(currentVersion, "currentVersion");
    }

    /** Returns the changes of one kind, in order. */
    public List<ApiChange> of(ApiChange.Kind kind) {
        return changes.stream().filter(change -> change.kind() == kind).toList();
    }

    /** Returns whether any change can break a client that worked with the baseline. */
    public boolean breaking() {
        return changes.stream().anyMatch(change -> change.kind().breaking());
    }
}
