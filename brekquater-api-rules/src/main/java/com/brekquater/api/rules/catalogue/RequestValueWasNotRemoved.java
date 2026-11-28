// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.Explanation;

/**
 * BQ-EVO-005: every value a client could send in the last release is still accepted. A value taken out of a
 * request enum turns working requests into errors.
 */
public final class RequestValueWasNotRemoved extends ChangeRule {

    @Override
    public String id() {
        return "BQ-EVO-005";
    }

    @Override
    ApiChange.Kind kind() {
        return ApiChange.Kind.REQUEST_VALUE_REMOVED;
    }

    @Override
    Explanation explain(ApiChange change) {
        return new Explanation(
                "The " + change.subject() + " no longer accepts " + change.detail() + ".",
                "Keep the enum constant, marked @Deprecated, and map it to its replacement on the server until the next major "
                        + "version.");
    }
}
