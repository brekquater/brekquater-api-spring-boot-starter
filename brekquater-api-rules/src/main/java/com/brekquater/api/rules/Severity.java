// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

/**
 * How serious a finding is. The constants are declared from most to least serious, and reports rely on that order.
 */
public enum Severity {
    /** Reserved for security rules. Strict mode fails on these. */
    BLOCKER,
    MAJOR,
    MINOR,
    /** Shown to the developer but never counted in the score. */
    INFO
}
