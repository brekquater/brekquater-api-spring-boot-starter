// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.AssessmentContext;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Evidence;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.FailedTest;
import com.brekquater.api.rules.Finding;
import com.brekquater.api.rules.Rule;
import com.brekquater.api.rules.RuleInput;
import com.brekquater.api.rules.RuleResult;
import com.brekquater.api.rules.Severity;
import com.brekquater.api.rules.TestResults;
import java.util.List;

/** BQ-TST-003: no test failed or ended with an error. Each test that ran is a target; skipped tests are not. */
public final class TestsPass implements Rule {

    @Override
    public String id() {
        return "BQ-TST-003";
    }

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    public Category category() {
        return Category.TESTING;
    }

    @Override
    public RuleInput input() {
        return RuleInput.TEST_RESULTS;
    }

    @Override
    public RuleResult evaluate(AssessmentContext context) {
        return context.testResults().map(this::evaluate).orElse(RuleResult.notApplicable());
    }

    private RuleResult evaluate(TestResults results) {
        List<Finding> findings = results.failed().stream().map(this::finding).toList();
        return RuleResult.of(results.executed(), findings);
    }

    private Finding finding(FailedTest test) {
        Explanation explanation = new Explanation(
                "Test " + test.location() + " failed or ended with an error.",
                "Fix the test or the code it tests, then build again so the test report is current.");
        return Finding.of(this, new Evidence(RuleInput.TEST_RESULTS, test.location()), explanation);
    }
}
