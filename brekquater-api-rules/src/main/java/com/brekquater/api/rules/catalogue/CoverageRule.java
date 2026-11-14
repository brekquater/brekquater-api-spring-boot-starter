// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.AssessmentContext;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Coverage;
import com.brekquater.api.rules.CoverageCounter;
import com.brekquater.api.rules.CoverageTargets;
import com.brekquater.api.rules.Evidence;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.Finding;
import com.brekquater.api.rules.ReportFile;
import com.brekquater.api.rules.Rule;
import com.brekquater.api.rules.RuleInput;
import com.brekquater.api.rules.RuleResult;
import java.util.List;

/**
 * Checks that one coverage counter reaches its target. The project is the single target, as in a quality gate, so
 * the rule raises at most one finding however many classes fall short.
 */
abstract class CoverageRule implements Rule {

    private static final String UNNAMED_REPORT = "coverage report";

    @Override
    public final Category category() {
        return Category.TESTING;
    }

    @Override
    public final RuleInput input() {
        return RuleInput.COVERAGE;
    }

    @Override
    public final RuleResult evaluate(AssessmentContext context) {
        return context.coverage().map(coverage -> evaluate(coverage, context)).orElse(RuleResult.notApplicable());
    }

    /** Returns the counter this rule checks, such as lines. */
    abstract CoverageCounter counter(Coverage coverage);

    abstract int target(CoverageTargets targets);

    /** Returns what is measured, starting with a capital, such as "Line coverage". */
    abstract String measure();

    /** Returns where the coverage is missing, as a sentence, or an empty string. */
    abstract String whereMissing(Coverage coverage);

    private RuleResult evaluate(Coverage coverage, AssessmentContext context) {
        CoverageCounter counter = counter(coverage);
        int target = target(context.coverageTargets());
        if (counter.total() == 0) {
            return RuleResult.notApplicable();
        }
        if (counter.meets(target)) {
            return RuleResult.of(1, List.of());
        }
        return RuleResult.of(1, List.of(Finding.of(this, evidenceIn(context), explain(coverage, target))));
    }

    private Explanation explain(Coverage coverage, int target) {
        String message = (measure() + " is " + counter(coverage).percent() + "%, below the target of " + target
                        + "%. " + whereMissing(coverage))
                .strip();
        return new Explanation(message, "Add tests for the code that no test runs yet, starting with the classes "
                + "that miss the most. Then build again, so the coverage report is current.");
    }

    private static Evidence evidenceIn(AssessmentContext context) {
        String report = context.reports().coverage().file().map(ReportFile::name).orElse(UNNAMED_REPORT);
        return new Evidence(RuleInput.COVERAGE, report);
    }
}
