// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiSecurityRequirement;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * BQ-SEC-002: every security requirement names schemes that the document defines. A requirement that names an
 * undefined scheme tells clients nothing about how to authenticate.
 */
public final class SecurityRequirementNamesDefinedScheme extends OpenApiRule<ApiSecurityRequirement> {

    @Override
    public String id() {
        return "BQ-SEC-002";
    }

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    public Category category() {
        return Category.SECURITY;
    }

    @Override
    protected List<ApiSecurityRequirement> targets(OpenApiContext document) {
        return DocumentTargets.securityRequirements(document);
    }

    @Override
    protected JsonPointer locate(ApiSecurityRequirement requirement) {
        return requirement.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiSecurityRequirement requirement, OpenApiContext document) {
        Set<String> undefined = new TreeSet<>(requirement.schemeNames());
        undefined.removeAll(document.securitySchemeNames());
        return undefined.isEmpty() ? Optional.empty() : Optional.of(undefinedSchemes(undefined));
    }

    private static Explanation undefinedSchemes(Set<String> names) {
        return new Explanation(
                "Security requirement names undefined schemes " + names + ".",
                "Define each scheme with @SecurityScheme(name = \"...\", ...) or on the OpenAPI bean under "
                        + "components.securitySchemes, using the same name.");
    }
}
