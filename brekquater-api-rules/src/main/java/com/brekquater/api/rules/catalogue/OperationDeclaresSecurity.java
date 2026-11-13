// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.ApiSecurityRequirement;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/**
 * BQ-SEC-001: every operation declares how callers authenticate, or is marked public on purpose with
 * {@code x-brekquater-public: true}.
 *
 * <p>An operation's own {@code security} replaces the document-wide one. A list that includes an empty entry
 * ({@code {}}) lets anonymous callers in, so it counts as undeclared too.
 */
public final class OperationDeclaresSecurity extends OpenApiRule<ApiOperation> {

    private static final Explanation UNSECURED = new Explanation(
            "Operation declares no security and is not marked public.",
            "Add @SecurityRequirement(name = \"...\") to the operation or declare security for the whole API on "
                    + "the OpenAPI bean. If the operation is public on purpose, mark it with the extension "
                    + "x-brekquater-public: true.");

    @Override
    public String id() {
        return "BQ-SEC-001";
    }

    @Override
    public Severity severity() {
        return Severity.BLOCKER;
    }

    @Override
    public Category category() {
        return Category.SECURITY;
    }

    @Override
    protected List<ApiOperation> targets(OpenApiContext document) {
        return document.operations();
    }

    @Override
    protected JsonPointer locate(ApiOperation operation) {
        return operation.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiOperation operation, OpenApiContext document) {
        List<ApiSecurityRequirement> effective = operation.declaredSecurity().orElse(document.rootSecurity());
        boolean secured = !effective.isEmpty()
                && effective.stream().noneMatch(ApiSecurityRequirement::allowsAnonymousAccess);
        return secured || operation.markedPublic() ? Optional.empty() : Optional.of(UNSECURED);
    }
}
