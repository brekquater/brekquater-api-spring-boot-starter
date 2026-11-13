// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

/**
 * A rule that threw instead of returning a result.
 *
 * @param ruleId the ID of the rule that failed
 * @param error the class name of what was thrown; never the message or stack trace, which could leak internals
 */
public record FailedRule(String ruleId, String error) {

    static FailedRule of(Rule rule, Throwable thrown) {
        return new FailedRule(rule.id(), thrown.getClass().getName());
    }
}
