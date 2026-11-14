// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.Finding;
import java.io.Serial;
import java.util.List;
import java.util.stream.Collectors;

/** Startup was stopped by strict mode, because the assessment has blocker findings. */
final class StrictModeViolationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    // Only the failure analyzer reads the blockers, in the same JVM; the exception is never serialised.
    private final transient List<Finding> blockers;

    StrictModeViolationException(List<Finding> blockers) {
        super("Strict mode stopped startup: " + blockers.size() + " blocker finding(s) from "
                + blockers.stream().map(Finding::ruleId).distinct().collect(Collectors.joining(", ")) + ".");
        this.blockers = List.copyOf(blockers);
    }

    List<Finding> blockers() {
        return blockers;
    }
}
