// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;

/**
 * One parameter of an operation.
 *
 * @param name the parameter name
 * @param location where the parameter is sent: {@code query}, {@code path}, {@code header} or {@code cookie}
 */
public record ApiParameter(String name, String location) {

    private static final String QUERY = "query";

    public ApiParameter {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(location, "location");
    }

    public boolean inQuery() {
        return QUERY.equals(location);
    }
}
