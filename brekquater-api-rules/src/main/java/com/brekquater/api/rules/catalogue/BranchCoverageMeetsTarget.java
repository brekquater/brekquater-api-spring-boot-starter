// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.Coverage;
import com.brekquater.api.rules.CoverageCounter;
import com.brekquater.api.rules.CoverageTargets;
import com.brekquater.api.rules.Severity;

/** BQ-TST-002: branch coverage reaches the target, 70% unless configured otherwise. */
public final class BranchCoverageMeetsTarget extends CoverageRule {

    @Override
    public String id() {
        return "BQ-TST-002";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    CoverageCounter counter(Coverage coverage) {
        return coverage.branches();
    }

    @Override
    int target(CoverageTargets targets) {
        return targets.branchCoverage();
    }

    @Override
    String measure() {
        return "Branch coverage";
    }

    @Override
    String whereMissing(Coverage coverage) {
        return "";
    }
}
