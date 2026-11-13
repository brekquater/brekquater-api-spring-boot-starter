// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiInfo;
import com.brekquater.api.rules.Explanation;
import java.util.Optional;

/** BQ-DOC-003: the document explains what the API is for. */
public final class ApiHasDescription extends InfoRule {

    private static final Explanation NO_DESCRIPTION = new Explanation(
            "API has no description.",
            "Declare an OpenAPI bean with new Info().description(\"...\"), or set "
                    + "@OpenAPIDefinition(info = @Info(description = \"...\")).");

    @Override
    public String id() {
        return "BQ-DOC-003";
    }

    @Override
    protected Optional<Explanation> check(ApiInfo info) {
        return hasText(info.description()) ? Optional.empty() : Optional.of(NO_DESCRIPTION);
    }
}
