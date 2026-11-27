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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BQ-DES-015: the API receives its version one way: a path segment, a header or a query parameter. Clients that
 * must send {@code /v2} to one operation and {@code X-API-Version: 2} to the next cannot share one client setup,
 * and Spring resolves the version with a single strategy. The most common mechanism is taken as the API's own.
 */
public final class OneVersioningMechanism extends OpenApiRule<ApiOperation> {

    @Override
    public String id() {
        return "BQ-DES-015";
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
    protected List<ApiOperation> targets(OpenApiContext document) {
        return document.operations().stream()
                .filter(operation -> !VersioningMechanism.of(operation).isEmpty())
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiOperation target) {
        return target.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiOperation target, OpenApiContext document) {
        VersioningMechanism common = mostCommon(targets(document));
        Set<VersioningMechanism> used = VersioningMechanism.of(target);
        if (used.equals(Set.of(common))) {
            return Optional.empty();
        }
        return Optional.of(new Explanation(
                "Operation receives its version through " + labels(used) + ", while most versioned operations use "
                        + common.label() + ".",
                "Choose one spring.mvc.apiversion.use.* property, and remove hand-written version parameters and "
                        + "path prefixes that use another mechanism."));
    }

    private static VersioningMechanism mostCommon(List<ApiOperation> operations) {
        Map<VersioningMechanism, Integer> counts = new EnumMap<>(VersioningMechanism.class);
        operations.forEach(operation -> VersioningMechanism.of(operation)
                .forEach(mechanism -> counts.merge(mechanism, 1, Integer::sum)));
        return counts.entrySet().stream()
                .max(Comparator.comparing(Map.Entry<VersioningMechanism, Integer>::getValue)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    private static String labels(Set<VersioningMechanism> mechanisms) {
        return mechanisms.stream().map(VersioningMechanism::label).collect(Collectors.joining(" and "));
    }
}
