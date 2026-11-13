// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;

/** The {@code x-brekquater-ignore} opt-outs declared in the assessed document. */
final class Suppressions {

    private static final String CHILD_SEPARATOR = "/";

    private final List<ApiOperation> operations;

    private Suppressions(List<ApiOperation> operations) {
        this.operations = operations;
    }

    static Suppressions in(AssessmentContext context) {
        return new Suppressions(context.openApi().map(OpenApiContext::operations).orElse(List.of()));
    }

    boolean suppresses(Finding finding) {
        return operations.stream()
                .filter(operation -> operation.ignoredRuleIds().contains(finding.ruleId()))
                .anyMatch(operation -> contains(operation, finding.evidence()));
    }

    // A finding on something inside the operation, such as one of its responses, is covered by the opt-out too.
    private static boolean contains(ApiOperation operation, Evidence evidence) {
        String operationLocation = operation.pointer().value();
        return evidence.source() == RuleInput.OPENAPI
                && (evidence.location().equals(operationLocation)
                        || evidence.location().startsWith(operationLocation + CHILD_SEPARATOR));
    }
}
