// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.evidence;

/** A build report that is malformed or not of the expected kind. */
public final class UnreadableReportException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    UnreadableReportException(String message) {
        super(message);
    }

    UnreadableReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
