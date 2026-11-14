// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ClassCoverage;
import com.brekquater.api.rules.Coverage;
import com.brekquater.api.rules.CoverageCounter;
import com.brekquater.api.rules.CoverageTargets;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.stream.Collectors;

/** BQ-TST-001: line coverage reaches the target, 80% unless configured otherwise. */
public final class LineCoverageMeetsTarget extends CoverageRule {

    // Three names point to where to start without turning one finding into a coverage report.
    private static final int CLASSES_NAMED = 3;

    @Override
    public String id() {
        return "BQ-TST-001";
    }

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    CoverageCounter counter(Coverage coverage) {
        return coverage.lines();
    }

    @Override
    int target(CoverageTargets targets) {
        return targets.lineCoverage();
    }

    @Override
    String measure() {
        return "Line coverage";
    }

    @Override
    String whereMissing(Coverage coverage) {
        List<ClassCoverage> worst = coverage.classesWithMostMissedLines(CLASSES_NAMED);
        if (worst.isEmpty()) {
            return "";
        }
        return worst.stream()
                .map(classCoverage -> classCoverage.name() + " (" + classCoverage.lines().missed() + ")")
                .collect(Collectors.joining(", ", "Most missed lines: ", "."));
    }
}
