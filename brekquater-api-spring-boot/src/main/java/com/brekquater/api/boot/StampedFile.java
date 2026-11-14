// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.ReportFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;

/**
 * A report file with the size and modification time it had when it was looked at.
 *
 * @param path where the file is
 * @param size its size in bytes
 * @param reportFile its name and modification time, the only part that is ever shown
 */
record StampedFile(Path path, long size, ReportFile reportFile) {

    /** Returns the file at {@code path}, or empty when there is no readable regular file there. */
    static Optional<StampedFile> of(Path path) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
            if (!attributes.isRegularFile()) {
                return Optional.empty();
            }
            ReportFile reportFile = new ReportFile(path.getFileName().toString(), attributes.lastModifiedTime().toInstant());
            return Optional.of(new StampedFile(path, attributes.size(), reportFile));
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    String fingerprint() {
        return path + ":" + size + ":" + reportFile.modifiedAt();
    }
}
