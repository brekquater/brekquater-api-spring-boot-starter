// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.Finding;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Explains a startup that strict mode stopped: which blockers were found, where, and how to proceed. */
@NullMarked
final class StrictModeFailureAnalyzer extends AbstractFailureAnalyzer<StrictModeViolationException> {

    // Enough to start fixing; the full list is in the assessment.
    private static final int LISTED = 10;

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, StrictModeViolationException cause) {
        List<Finding> blockers = cause.blockers();
        String description = "Strict mode (brekquater.api.strict=true) found " + blockers.size()
                + " blocker finding(s):" + System.lineSeparator() + listed(blockers);
        String action = "Fix each blocker; every finding links to its rule page. Or suppress a finding on its "
                + "operation with x-brekquater-ignore, or turn strict mode off with brekquater.api.strict=false.";
        return new FailureAnalysis(description, action, cause);
    }

    private static String listed(List<Finding> blockers) {
        String lines = blockers.stream()
                .limit(LISTED)
                .map(finding -> "  - " + finding.ruleId() + " at " + finding.evidence().location() + ": "
                        + finding.message())
                .collect(Collectors.joining(System.lineSeparator()));
        int unlisted = blockers.size() - LISTED;
        return unlisted > 0 ? lines + System.lineSeparator() + "  ... and " + unlisted + " more" : lines;
    }
}
