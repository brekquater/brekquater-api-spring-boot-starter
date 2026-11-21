// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * One entry of a {@code security} list: the schemes that together satisfy it, and the scopes it asks of each.
 *
 * @param pointer the location of the entry in the document
 * @param scopes the scopes required of each scheme, by scheme name, in name order; empty for {@code {}}, which
 *     allows anonymous access. A scheme that needs no scopes maps to an empty list.
 */
public record ApiSecurityRequirement(JsonPointer pointer, Map<String, List<String>> scopes) {

    public ApiSecurityRequirement {
        Objects.requireNonNull(pointer, "pointer");
        Map<String, List<String>> sorted = new TreeMap<>();
        scopes.forEach((scheme, required) -> sorted.put(scheme, List.copyOf(required)));
        scopes = Collections.unmodifiableMap(sorted);
    }

    /** Returns an entry that requires the named schemes without asking for scopes. */
    public ApiSecurityRequirement(JsonPointer pointer, Set<String> schemeNames) {
        this(pointer, schemeNames.stream().collect(Collectors.toMap(name -> name, name -> List.<String>of())));
    }

    /** Returns the names of the security schemes the entry requires. */
    public Set<String> schemeNames() {
        return scopes.keySet();
    }

    public boolean allowsAnonymousAccess() {
        return scopes.isEmpty();
    }
}
