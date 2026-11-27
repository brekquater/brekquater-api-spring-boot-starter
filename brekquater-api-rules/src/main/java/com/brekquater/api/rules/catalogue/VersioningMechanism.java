// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.ApiParameter;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/** The ways an operation can receive its API version that the document shows. A media-type parameter never does. */
enum VersioningMechanism {
    PATH("a path segment"),
    HEADER("a header"),
    QUERY("a query parameter");

    private static final String VERSION = "version";

    private final String label;

    VersioningMechanism(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }

    static Set<VersioningMechanism> of(ApiOperation operation) {
        Set<VersioningMechanism> used = EnumSet.noneOf(VersioningMechanism.class);
        if (VersionSegment.in(operation.path()).isPresent()) {
            used.add(PATH);
        }
        operation.parameters().stream().filter(VersioningMechanism::namesVersion).forEach(parameter ->
                used.add(parameter.inQuery() ? QUERY : HEADER));
        return used;
    }

    // X-API-Version, Api-Version, Accept-Version or version, in a header or the query string; never a body field.
    private static boolean namesVersion(ApiParameter parameter) {
        boolean located = parameter.inQuery() || parameter.location().equals("header");
        String name = parameter.name().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        return located && name.endsWith(VERSION);
    }
}
