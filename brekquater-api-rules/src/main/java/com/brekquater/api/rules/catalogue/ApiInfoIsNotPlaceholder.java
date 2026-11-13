// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiInfo;
import com.brekquater.api.rules.Explanation;
import java.util.Optional;

/**
 * BQ-DOC-004: the API has a real name and version. springdoc fills in "OpenAPI definition" and "v0" when the
 * application declares neither, and those placeholders tell a reader nothing.
 */
public final class ApiInfoIsNotPlaceholder extends InfoRule {

    private static final String PLACEHOLDER_TITLE = "OpenAPI definition";
    private static final String PLACEHOLDER_VERSION = "v0";

    private static final Explanation PLACEHOLDER = new Explanation(
            "API title or version is missing or still springdoc's placeholder.",
            "Declare an OpenAPI bean with new Info().title(\"...\").version(\"...\"), or set "
                    + "@OpenAPIDefinition(info = @Info(title = \"...\", version = \"...\")).");

    @Override
    public String id() {
        return "BQ-DOC-004";
    }

    @Override
    protected Optional<Explanation> check(ApiInfo info) {
        boolean named = isReal(info.title(), PLACEHOLDER_TITLE) && isReal(info.version(), PLACEHOLDER_VERSION);
        return named ? Optional.empty() : Optional.of(PLACEHOLDER);
    }

    private static boolean isReal(Optional<String> value, String placeholder) {
        return hasText(value) && !value.orElseThrow().strip().equals(placeholder);
    }
}
