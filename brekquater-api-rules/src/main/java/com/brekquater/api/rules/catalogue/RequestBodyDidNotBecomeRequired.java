// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.ApiChanges;
import com.brekquater.api.rules.Explanation;
import java.util.List;
import java.util.stream.Stream;

/**
 * BQ-EVO-003: the request body did not become required, and no request property became required or was added as
 * required. Clients built for the last release leave it out, and their requests are now rejected.
 */
public final class RequestBodyDidNotBecomeRequired extends ChangeRule {

    @Override
    public String id() {
        return "BQ-EVO-003";
    }

    @Override
    ApiChange.Kind kind() {
        return ApiChange.Kind.PROPERTY_BECAME_REQUIRED;
    }

    @Override
    List<ApiChange> reported(ApiChanges changes) {
        return Stream.concat(changes.of(ApiChange.Kind.REQUEST_BODY_BECAME_REQUIRED).stream(), changes.of(kind()).stream())
                .sorted()
                .toList();
    }

    @Override
    Explanation explain(ApiChange change) {
        return new Explanation(
                "The " + change.subject() + " is required now, but was optional or absent in the baseline.",
                "Remove @NotNull or @Schema(requiredMode = REQUIRED) from the field and apply a default on the server, or publish "
                        + "the change as a new major version.");
    }
}
