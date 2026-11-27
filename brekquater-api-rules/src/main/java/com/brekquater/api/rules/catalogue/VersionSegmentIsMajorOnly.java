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
import java.util.List;
import java.util.Optional;

/**
 * BQ-NAM-007: a version in the path names the major version only, as in {@code /v2/orders}. A minor version
 * changes nothing a client must adapt to, so putting it in the URL breaks every client link for a compatible
 * release.
 */
public final class VersionSegmentIsMajorOnly extends OpenApiRule<ApiPath> {

    @Override
    public String id() {
        return "BQ-NAM-007";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public Category category() {
        return Category.NAMING;
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
        VersionSegment segment = VersionSegment.in(target.path()).orElseThrow();
        if (!segment.minorIncluded()) {
            return Optional.empty();
        }
        return Optional.of(new Explanation(
                "Path version " + segment.text() + " includes a minor version.",
                "Keep only the major version in the path, for example /v" + segment.major() + ", and put the full "
                        + "version in info.version."));
    }
}
