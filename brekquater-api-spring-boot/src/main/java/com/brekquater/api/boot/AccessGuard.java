// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

/**
 * Decides whether the caller of the current request may see the assessment (Strategy pattern). It is checked
 * inside the Brekquater handlers, so the starter never has to register or change a security filter chain.
 */
public interface AccessGuard {

    boolean admitsCurrentCaller();

    /** Returns a guard that admits everyone, for the {@code public} panel mode. */
    static AccessGuard everyone() {
        return () -> true;
    }
}
