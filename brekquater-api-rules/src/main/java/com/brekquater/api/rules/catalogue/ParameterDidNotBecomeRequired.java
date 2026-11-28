// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.Explanation;

/**
 * BQ-EVO-002: no parameter became required, and no required parameter was added. Clients that worked with the
 * last release do not send it, so every one of their calls now fails.
 */
public final class ParameterDidNotBecomeRequired extends ChangeRule {

    @Override
    public String id() {
        return "BQ-EVO-002";
    }

    @Override
    ApiChange.Kind kind() {
        return ApiChange.Kind.PARAMETER_BECAME_REQUIRED;
    }

    @Override
    Explanation explain(ApiChange change) {
        return new Explanation(
                "The " + change.subject() + " is required now, but was optional or absent in the baseline.",
                "Make it optional with a default, for example @RequestParam(required = false, defaultValue = \"20\"), or publish "
                        + "the change as a new major version.");
    }
}
