// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.evidence;

import java.io.Serial;

/**
 * A build report that is malformed or not of the expected kind. The message says why in one sentence that never
 * quotes the report, so it can be logged.
 */
public final class UnreadableReportException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnreadableReportException(String message) {
        super(message);
    }

    UnreadableReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
