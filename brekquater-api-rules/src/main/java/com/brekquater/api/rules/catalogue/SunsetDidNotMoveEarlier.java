// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.Severity;

/**
 * BQ-EVO-009: an {@code x-sunset} date does not move earlier than the last release promised. Clients plan their
 * migration around the announced date.
 */
public final class SunsetDidNotMoveEarlier extends ChangeRule {

    @Override
    public String id() {
        return "BQ-EVO-009";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    ApiChange.Kind kind() {
        return ApiChange.Kind.SUNSET_MOVED_EARLIER;
    }

    @Override
    Explanation explain(ApiChange change) {
        return new Explanation(
                "The sunset moved earlier, from " + change.detail() + ".",
                "Keep the date the last release announced, or later.");
    }
}
