// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.Explanation;

/**
 * BQ-EVO-004: a property every response had in the last release is still returned. Clients read it without
 * checking, so removing it breaks them.
 */
public final class ResponsePropertyWasNotRemoved extends ChangeRule {

    @Override
    public String id() {
        return "BQ-EVO-004";
    }

    @Override
    ApiChange.Kind kind() {
        return ApiChange.Kind.RESPONSE_PROPERTY_REMOVED;
    }

    @Override
    Explanation explain(ApiChange change) {
        return new Explanation(
                "Responses no longer have the " + change.subject() + ", which the baseline marked required.",
                "Keep the field, marked @Deprecated, until the next major version.");
    }
}
