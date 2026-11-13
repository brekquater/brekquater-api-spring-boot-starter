// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.report;

import java.io.Writer;

/** Writes an assessment in one output format. */
public interface ReportWriter {

    void write(Assessment assessment, Writer output);

    /** Returns the writer of the versioned JSON contract ({@code schemaVersion}). */
    static ReportWriter json() {
        return new JsonReportWriter();
    }
}
