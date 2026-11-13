// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiPath;
import com.brekquater.api.rules.Explanation;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * BQ-NAM-001: the fixed parts of a path are lowercase words joined by hyphens, such as {@code order-items}.
 * Segments that hold a path parameter are not checked.
 */
public final class PathSegmentsAreKebabCase extends PathRule {

    private static final Pattern KEBAB_CASE = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    @Override
    public String id() {
        return "BQ-NAM-001";
    }

    @Override
    protected Optional<Explanation> check(ApiPath path) {
        List<String> offending = path.literalSegments().stream()
                .filter(segment -> !KEBAB_CASE.matcher(segment).matches())
                .toList();
        return offending.isEmpty() ? Optional.empty() : Optional.of(notKebabCase(offending));
    }

    private static Explanation notKebabCase(List<String> segments) {
        return new Explanation(
                "Path segments " + segments + " are not lowercase kebab-case.",
                "Rename the segments in @RequestMapping, for example /order-items instead of /orderItems.");
    }
}
