// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiPath;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/** Base class for naming rules whose targets are the keys under {@code paths}. */
abstract class PathRule extends OpenApiRule<ApiPath> {

    @Override
    public final Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public final Category category() {
        return Category.NAMING;
    }

    @Override
    protected final List<ApiPath> targets(OpenApiContext document) {
        return document.paths();
    }

    @Override
    protected final JsonPointer locate(ApiPath path) {
        return path.pointer();
    }

    @Override
    protected final Optional<Explanation> check(ApiPath path, OpenApiContext document) {
        return check(path);
    }

    protected abstract Optional<Explanation> check(ApiPath path);
}
