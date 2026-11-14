// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;
import java.util.Optional;

/**
 * One report from the application's build, and whether it could be used.
 *
 * @param status whether the report was read
 * @param content what the report says; present exactly when it was read
 * @param file the file it came from; absent when no file was found
 * @param <T> what the report describes, such as {@link Coverage}
 */
public record BuildReport<T>(ReportStatus status, Optional<T> content, Optional<ReportFile> file) {

    public BuildReport {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(file, "file");
        if (content.isPresent() != (status == ReportStatus.READ)) {
            throw new IllegalArgumentException("A report has content exactly when it was read, but it is " + status);
        }
    }

    public static <T> BuildReport<T> read(T content, ReportFile file) {
        return new BuildReport<>(ReportStatus.READ, Optional.of(content), Optional.of(file));
    }

    public static <T> BuildReport<T> missing() {
        return new BuildReport<>(ReportStatus.MISSING, Optional.empty(), Optional.empty());
    }

    public static <T> BuildReport<T> unreadable(ReportFile file) {
        return new BuildReport<>(ReportStatus.UNREADABLE, Optional.empty(), Optional.of(file));
    }
}
