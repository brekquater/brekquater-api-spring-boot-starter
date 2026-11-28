// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.Explanation;

/**
 * BQ-EVO-006: every success status code of the last release is still documented. Clients branch on the status,
 * so a 200 that became a 201 sends them down their error path.
 */
public final class SuccessStatusWasNotRemoved extends ChangeRule {

    @Override
    public String id() {
        return "BQ-EVO-006";
    }

    @Override
    ApiChange.Kind kind() {
        return ApiChange.Kind.SUCCESS_STATUS_REMOVED;
    }

    @Override
    Explanation explain(ApiChange change) {
        return new Explanation(
                "Operation no longer documents " + change.subject() + ", which the baseline did.",
                "Return the same status as before, or document both while clients move, for example with two @ApiResponse "
                        + "annotations.");
    }
}
