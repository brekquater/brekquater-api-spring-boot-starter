// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

/** Whether a build report could be used. */
public enum ReportStatus {
    /** The report was found and read. */
    READ,
    /** No report was found where it was looked for. */
    MISSING,
    /** A report was found but could not be read, for example because it is malformed or too large. */
    UNREADABLE
}
