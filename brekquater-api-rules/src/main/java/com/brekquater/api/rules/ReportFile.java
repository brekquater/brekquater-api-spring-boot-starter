// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.time.Instant;
import java.util.Objects;

/**
 * Which file a build report came from. Only the file name is kept, never its directory, so a report can be shown
 * without revealing the layout of the machine it was read on.
 *
 * @param name the file or directory name, without any parent path
 * @param modifiedAt when the file was last written, which tells the reader how old the evidence is
 */
public record ReportFile(String name, Instant modifiedAt) {

    public ReportFile {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(modifiedAt, "modifiedAt");
    }
}
