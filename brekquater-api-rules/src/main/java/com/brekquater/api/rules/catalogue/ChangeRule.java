// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.ApiChanges;
import com.brekquater.api.rules.AssessmentContext;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Evidence;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.Finding;
import com.brekquater.api.rules.Rule;
import com.brekquater.api.rules.RuleInput;
import com.brekquater.api.rules.RuleResult;
import com.brekquater.api.rules.Severity;
import java.util.List;

/**
 * Base class for rules that report one kind of change since the baseline document. Every operation of the
 * baseline is a target, because each one was checked for the change.
 */
abstract class ChangeRule implements Rule {

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    public final Category category() {
        return Category.DESIGN;
    }

    @Override
    public final RuleInput input() {
        return RuleInput.BASELINE;
    }

    @Override
    public final RuleResult evaluate(AssessmentContext context) {
        return context.changes().map(this::evaluate).orElse(RuleResult.notApplicable());
    }

    /** Returns the kind of change this rule reports. */
    abstract ApiChange.Kind kind();

    /** Returns the changes this rule reports, in order: those of {@link #kind()} unless a rule says otherwise. */
    List<ApiChange> reported(ApiChanges changes) {
        return changes.of(kind());
    }

    abstract Explanation explain(ApiChange change);

    private RuleResult evaluate(ApiChanges changes) {
        List<Finding> findings = reported(changes).stream()
                .map(change -> Finding.of(this, new Evidence(RuleInput.BASELINE, change.pointer().value()), explain(change)))
                .toList();
        return RuleResult.of(changes.comparedOperations(), findings);
    }
}
