// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiInfo;
import com.brekquater.api.rules.Explanation;
import java.util.Optional;

/** BQ-DOC-002: the document says who to contact about the API. */
public final class ApiDeclaresContact extends InfoRule {

    private static final Explanation NO_CONTACT = new Explanation(
            "API documents no contact.",
            "Declare an OpenAPI bean with new Info().contact(new Contact().name(\"...\").email(\"...\")), or set "
                    + "@OpenAPIDefinition(info = @Info(contact = @Contact(...))).");

    @Override
    public String id() {
        return "BQ-DOC-002";
    }

    @Override
    protected Optional<Explanation> check(ApiInfo info) {
        return info.hasContact() ? Optional.empty() : Optional.of(NO_CONTACT);
    }
}
