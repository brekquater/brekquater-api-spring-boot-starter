// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiPath;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * BQ-DES-016: every version path segment sits at the same position, as in {@code /api/v1/orders} and
 * {@code /api/v2/customers}. Spring's path-segment versioning reads the version from one fixed index, and clients
 * build URLs from one base path. The most common position is taken as the API's own.
 */
public final class VersionSegmentsShareOneIndex extends OpenApiRule<ApiPath> {

    @Override
    public String id() {
        return "BQ-DES-016";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public Category category() {
        return Category.DESIGN;
    }

    @Override
    protected List<ApiPath> targets(OpenApiContext document) {
        return document.paths().stream().filter(path -> VersionSegment.in(path.path()).isPresent()).toList();
    }

    @Override
    protected JsonPointer locate(ApiPath target) {
        return target.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiPath target, OpenApiContext document) {
        int common = mostCommonIndex(targets(document));
        VersionSegment segment = VersionSegment.in(target.path()).orElseThrow();
        if (segment.index() == common) {
            return Optional.empty();
        }
        return Optional.of(new Explanation(
                "Path version " + segment.text() + " is segment " + segment.index() + ", while most versioned paths "
                        + "have it at segment " + common + ".",
                "Give every controller the same prefix, for example @RequestMapping(\"/api/{version}/...\") with "
                        + "spring.mvc.apiversion.use.path-segment, or PathMatchConfigurer.addPathPrefix."));
    }

    // Ties go to the earlier position, so the result does not depend on the order of the paths.
    private static int mostCommonIndex(List<ApiPath> paths) {
        Map<Integer, Integer> counts = new TreeMap<>();
        paths.forEach(path -> counts.merge(VersionSegment.in(path.path()).orElseThrow().index(), 1, Integer::sum));
        return counts.entrySet().stream()
                .max(Comparator.comparing(Map.Entry<Integer, Integer>::getValue)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElseThrow();
    }
}
