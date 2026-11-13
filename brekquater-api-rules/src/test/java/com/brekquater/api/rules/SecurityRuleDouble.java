// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

/** Test double: a security rule that checks nothing. */
record SecurityRuleDouble(String id) implements Rule {

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    public Category category() {
        return Category.SECURITY;
    }

    @Override
    public RuleInput input() {
        return RuleInput.OPENAPI;
    }

    @Override
    public RuleResult evaluate(AssessmentContext context) {
        return RuleResult.notApplicable();
    }
}
