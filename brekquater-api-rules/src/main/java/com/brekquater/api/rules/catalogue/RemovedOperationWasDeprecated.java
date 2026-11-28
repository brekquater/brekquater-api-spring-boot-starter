// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.Explanation;

/**
 * BQ-EVO-001: an operation of the last release is only removed after it was deprecated. Clients that still call it
 * fail without warning otherwise.
 */
public final class RemovedOperationWasDeprecated extends ChangeRule {

    @Override
    public String id() {
        return "BQ-EVO-001";
    }

    @Override
    ApiChange.Kind kind() {
        return ApiChange.Kind.OPERATION_REMOVED;
    }

    @Override
    Explanation explain(ApiChange change) {
        return new Explanation(
                change.subject() + " was removed, but the baseline did not mark it deprecated" + ".",
                "Restore the mapping and mark it @Deprecated or @Operation(deprecated = true) for at least one release, then remove "
                        + "it.");
    }
}
