// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiPath;
import com.brekquater.api.rules.Explanation;
import java.util.Optional;

/**
 * BQ-NAM-002: a path does not end with a slash. {@code /orders} and {@code /orders/} look like one resource but
 * many servers, caches and clients treat them as two.
 */
public final class PathHasNoTrailingSlash extends PathRule {

    private static final String ROOT = "/";

    private static final Explanation TRAILING_SLASH = new Explanation(
            "Path ends with a slash.",
            "Remove the trailing slash from the mapping, for example @GetMapping(\"/orders\").");

    @Override
    public String id() {
        return "BQ-NAM-002";
    }

    @Override
    protected Optional<Explanation> check(ApiPath path) {
        boolean trailingSlash = !path.path().equals(ROOT) && path.path().endsWith(ROOT);
        return trailingSlash ? Optional.of(TRAILING_SLASH) : Optional.empty();
    }
}
