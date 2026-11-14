// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * What a coverage report says about the whole project.
 *
 * @param lines the project's line counter
 * @param branches the project's branch counter
 * @param classes the line coverage of each class
 */
public record Coverage(CoverageCounter lines, CoverageCounter branches, List<ClassCoverage> classes) {

    private static final Comparator<ClassCoverage> MOST_MISSED_FIRST = Comparator
            .comparingInt((ClassCoverage coverage) -> coverage.lines().missed())
            .reversed()
            .thenComparing(ClassCoverage::name);

    public Coverage {
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(branches, "branches");
        classes = List.copyOf(classes);
    }

    /** Returns up to {@code limit} classes with missed lines, those with the most missed lines first. */
    public List<ClassCoverage> classesWithMostMissedLines(int limit) {
        return classes.stream()
                .filter(coverage -> coverage.lines().missed() > 0)
                .sorted(MOST_MISSED_FIRST)
                .limit(limit)
                .toList();
    }
}
