// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;

/**
 * The line coverage of one class.
 *
 * @param name the fully qualified class name, with dots
 * @param lines the class's line counter
 */
public record ClassCoverage(String name, CoverageCounter lines) {

    public ClassCoverage {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lines, "lines");
    }
}
