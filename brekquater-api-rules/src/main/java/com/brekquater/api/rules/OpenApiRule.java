// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;
import java.util.Optional;

/**
 * Base class for rules that read the OpenAPI document (Template Method pattern). It owns the walk over the targets
 * and the creation of findings; a subclass names its targets and judges one target at a time.
 *
 * @param <T> what the rule checks, such as an operation, a path or a response
 */
public abstract class OpenApiRule<T> implements Rule {

    @Override
    public final RuleInput input() {
        return RuleInput.OPENAPI;
    }

    @Override
    public final RuleResult evaluate(AssessmentContext context) {
        return context.openApi().map(this::evaluate).orElse(RuleResult.notApplicable());
    }

    /** Returns everything this rule applies to in the document; their number is the rule's applicable targets. */
    protected abstract List<T> targets(OpenApiContext document);

    /** Returns where the target is in the document, used as the evidence of a finding. */
    protected abstract JsonPointer locate(T target);

    /** Returns what is wrong with the target, or empty when it passes. */
    protected abstract Optional<Explanation> check(T target, OpenApiContext document);

    private RuleResult evaluate(OpenApiContext document) {
        List<T> targets = targets(document);
        List<Finding> findings = targets.stream()
                .flatMap(target -> check(target, document).map(explanation -> findingAt(target, explanation)).stream())
                .toList();
        return RuleResult.of(targets.size(), findings);
    }

    private Finding findingAt(T target, Explanation explanation) {
        return Finding.of(this, Evidence.at(locate(target)), explanation);
    }
}
