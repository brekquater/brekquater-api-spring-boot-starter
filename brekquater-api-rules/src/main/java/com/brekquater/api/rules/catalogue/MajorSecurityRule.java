// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;

/** Base class for the security rules whose findings are major: a real weakness, but not a blocker. */
abstract class MajorSecurityRule<T> extends OpenApiRule<T> {

    @Override
    public final Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    public final Category category() {
        return Category.SECURITY;
    }
}
