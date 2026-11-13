// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.HttpMethod;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/**
 * BQ-DES-002: a GET takes no request body. HTTP gives a GET body no meaning, and proxies, caches and client
 * libraries may drop it.
 */
public final class GetHasNoRequestBody extends OpenApiRule<ApiOperation> {

    private static final Explanation BODY_ON_GET = new Explanation(
            "GET operation declares a request body.",
            "Move the input to query parameters, for example with @ParameterObject, or use POST if the input is "
                    + "too large for a URL.");

    @Override
    public String id() {
        return "BQ-DES-002";
    }

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    public Category category() {
        return Category.DESIGN;
    }

    @Override
    protected List<ApiOperation> targets(OpenApiContext document) {
        return document.operations().stream().filter(operation -> operation.method() == HttpMethod.GET).toList();
    }

    @Override
    protected JsonPointer locate(ApiOperation operation) {
        return operation.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiOperation operation, OpenApiContext document) {
        return operation.hasRequestBody() ? Optional.of(BODY_ON_GET) : Optional.empty();
    }
}
