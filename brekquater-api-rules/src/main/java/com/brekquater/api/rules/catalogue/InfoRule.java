// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiInfo;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/** Base class for documentation rules whose single target is the document's {@code info} object. */
abstract class InfoRule extends OpenApiRule<ApiInfo> {

    @Override
    public final Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public final Category category() {
        return Category.DOCUMENTATION;
    }

    @Override
    protected final List<ApiInfo> targets(OpenApiContext document) {
        return List.of(document.info());
    }

    @Override
    protected final JsonPointer locate(ApiInfo info) {
        return info.pointer();
    }

    @Override
    protected final Optional<Explanation> check(ApiInfo info, OpenApiContext document) {
        return check(info);
    }

    protected abstract Optional<Explanation> check(ApiInfo info);

    static boolean hasText(Optional<String> value) {
        return value.filter(text -> !text.isBlank()).isPresent();
    }
}
