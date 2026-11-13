// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;

/** Test double: a rule that checks ten targets and reports a finding at each of the given locations. */
record FailingAtRule(String id, Severity severity, List<String> failingLocations) implements Rule {

    static final int APPLICABLE_TARGETS = 10;

    private static final Explanation EXPLANATION = new Explanation("Something is wrong.", "Fix it.");

    @Override
    public Category category() {
        return Category.DESIGN;
    }

    @Override
    public RuleInput input() {
        return RuleInput.OPENAPI;
    }

    @Override
    public RuleResult evaluate(AssessmentContext context) {
        List<Finding> findings = failingLocations.stream()
                .map(location -> Finding.of(this, new Evidence(RuleInput.OPENAPI, location), EXPLANATION))
                .toList();
        return RuleResult.of(APPLICABLE_TARGETS, findings);
    }
}
