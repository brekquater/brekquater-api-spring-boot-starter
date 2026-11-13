// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;
import java.util.Set;

/**
 * One entry of a {@code security} list: the schemes that together satisfy it.
 *
 * @param pointer the location of the entry in the document
 * @param schemeNames the names of the security schemes the entry requires; empty for {@code {}}, which allows
 *     anonymous access
 */
public record ApiSecurityRequirement(JsonPointer pointer, Set<String> schemeNames) {

    public ApiSecurityRequirement {
        Objects.requireNonNull(pointer, "pointer");
        schemeNames = Set.copyOf(schemeNames);
    }

    public boolean allowsAnonymousAccess() {
        return schemeNames.isEmpty();
    }
}
