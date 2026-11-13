// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiPath;
import com.brekquater.api.rules.Explanation;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * BQ-NAM-003: a path names resources, not the create, read, update or delete action; the HTTP method already says
 * which action is meant.
 *
 * <p>A segment fails when its first word is on a closed list of CRUD verbs, so {@code getOrders} and
 * {@code delete-order} fail while {@code addresses} and {@code listings} pass. A verb later in the segment is not
 * checked, because words such as "update" are often nouns there.
 */
public final class PathHasNoCrudVerb extends PathRule {

    private static final Set<String> CRUD_VERBS = Set.of(
            "get", "create", "update", "delete", "add", "remove", "fetch", "list", "insert", "save", "retrieve",
            "modify");

    // Splits at hyphens, underscores, dots, and where a lowercase letter or digit is followed by an uppercase one.
    private static final Pattern WORD_BOUNDARY = Pattern.compile("[-_.]|(?<=[a-z0-9])(?=[A-Z])");

    @Override
    public String id() {
        return "BQ-NAM-003";
    }

    @Override
    protected Optional<Explanation> check(ApiPath path) {
        List<String> offending = path.literalSegments().stream().filter(PathHasNoCrudVerb::startsWithVerb).toList();
        return offending.isEmpty() ? Optional.empty() : Optional.of(crudVerbs(offending));
    }

    private static boolean startsWithVerb(String segment) {
        String firstWord = WORD_BOUNDARY.split(segment, 2)[0].toLowerCase(Locale.ROOT);
        return CRUD_VERBS.contains(firstWord);
    }

    private static Explanation crudVerbs(List<String> segments) {
        return new Explanation(
                "Path segments " + segments + " name an action instead of a resource.",
                "Name the resource and let the HTTP method carry the action, for example DELETE /orders/{id} "
                        + "instead of POST /orders/{id}/delete.");
    }
}
