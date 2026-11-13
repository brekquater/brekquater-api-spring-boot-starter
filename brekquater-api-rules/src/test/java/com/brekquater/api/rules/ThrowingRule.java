// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

/** Test double: a rule with a bug that makes it throw. */
record ThrowingRule(String id) implements Rule {

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

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
        throw new IllegalStateException("secret internal detail");
    }
}
