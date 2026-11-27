// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiChange;
import com.brekquater.api.rules.Explanation;

/**
 * BQ-EVO-007: no parameter, body or property changed to an incompatible type. Integer and number may replace each
 * other; a string that became an integer, or an object that became an array, breaks every client.
 */
public final class TypeDidNotChange extends ChangeRule {

    @Override
    public String id() {
        return "BQ-EVO-007";
    }

    @Override
    ApiChange.Kind kind() {
        return ApiChange.Kind.TYPE_CHANGED;
    }

    @Override
    Explanation explain(ApiChange change) {
        return new Explanation(
                "The " + change.subject() + " changed type from " + change.detail() + ".",
                "Keep the old type and add a new field or parameter with the new one, or publish the change as a new major "
                        + "version.");
    }
}
