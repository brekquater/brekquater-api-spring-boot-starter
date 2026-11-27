// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A literal path segment that names an API version, such as {@code v2}, {@code v1beta} or the {@code 2} that
 * springdoc writes for Spring's path-segment versioning. A template such as {@code {version}} is not one, because
 * its value is unknown.
 *
 * @param index the position of the segment in the path, counting from 0 after the leading slash
 * @param text the segment as written
 * @param major the major version it names
 * @param minorIncluded whether the segment also carries a minor or patch number, as in {@code v1.2}
 */
record VersionSegment(int index, String text, int major, boolean minorIncluded) {

    // ponytail: digits up to 6 only, so a long numeric literal (an ID baked into a path) is not read as a version.
    private static final Pattern VERSION =
            Pattern.compile("(?i)v?(\\d{1,6})(\\.\\d+){0,2}(?:(?:alpha|beta|preview)\\d*)?");

    /** Returns the first version segment of the path, if it has one. */
    static Optional<VersionSegment> in(String path) {
        List<String> segments = List.of(path.replaceFirst("^/", "").split("/"));
        for (int index = 0; index < segments.size(); index++) {
            Matcher matcher = VERSION.matcher(segments.get(index));
            if (matcher.matches()) {
                return Optional.of(new VersionSegment(
                        index, segments.get(index), Integer.parseInt(matcher.group(1)), matcher.group(2) != null));
            }
        }
        return Optional.empty();
    }
}
