// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;

/**
 * What a rule tells the developer about one failing target.
 *
 * @param message what is wrong, in plain language
 * @param remediation how to fix it in a Spring application
 */
public record Explanation(String message, String remediation) {

    public Explanation {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(remediation, "remediation");
    }
}
