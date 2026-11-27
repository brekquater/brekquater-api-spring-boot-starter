// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/**
 * BQ-DES-017: when the paths carry several major versions, the operations of the older ones are deprecated, so
 * clients see where to move before the old version is removed. INFO, because some APIs keep two versions live on
 * purpose.
 */
public final class OlderVersionsAreDeprecated extends OpenApiRule<ApiOperation> {

    @Override
    public String id() {
        return "BQ-DES-017";
    }

    @Override
    public Severity severity() {
        return Severity.INFO;
    }

    @Override
    public Category category() {
        return Category.DESIGN;
    }

    @Override
    protected List<ApiOperation> targets(OpenApiContext document) {
        List<ApiOperation> versioned = document.operations().stream()
                .filter(operation -> VersionSegment.in(operation.path()).isPresent())
                .toList();
        long majors = versioned.stream().map(OlderVersionsAreDeprecated::major).distinct().count();
        return majors > 1 ? versioned : List.of();
    }

    @Override
    protected JsonPointer locate(ApiOperation target) {
        return target.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiOperation target, OpenApiContext document) {
        int newest = targets(document).stream().mapToInt(OlderVersionsAreDeprecated::major).max().orElseThrow();
        if (major(target) == newest || target.deprecated()) {
            return Optional.empty();
        }
        return Optional.of(new Explanation(
                "Operation belongs to version " + major(target) + ", which is older than version " + newest
                        + ", but is not deprecated.",
                "Mark the handler @Deprecated or @Operation(deprecated = true). Spring can also send Deprecation and "
                        + "Sunset headers for the version: ApiVersionConfigurer.setDeprecationHandler(new "
                        + "StandardApiVersionDeprecationHandler())."));
    }

    private static int major(ApiOperation operation) {
        return VersionSegment.in(operation.path()).orElseThrow().major();
    }
}
