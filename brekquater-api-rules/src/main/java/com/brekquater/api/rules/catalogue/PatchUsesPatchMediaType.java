// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiMediaType;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.HttpMethod;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * BQ-DES-008: a {@code PATCH} body is a patch document. A plain {@code application/json} body does not say what a
 * missing field or a {@code null} means; JSON Merge Patch (RFC 7396) and JSON Patch (RFC 6902) do.
 */
public final class PatchUsesPatchMediaType extends OpenApiRule<ApiMediaType> {

    private static final Set<String> PATCH_DOCUMENTS = Set.of("application/merge-patch+json", "application/json-patch+json");
    private static final String PARAMETERS = ";";
    private static final Explanation NOT_A_PATCH = new Explanation(
            "PATCH body is not a patch document, so it does not say how missing or null fields are applied.",
            "Accept JSON Merge Patch with @PatchMapping(consumes = \"application/merge-patch+json\"); Spring reads "
                    + "it into the same types as application/json.");

    @Override
    public String id() {
        return "BQ-DES-008";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public Category category() {
        return Category.DESIGN;
    }

    @Override
    protected List<ApiMediaType> targets(OpenApiContext document) {
        return document.operations().stream()
                .filter(operation -> operation.method() == HttpMethod.PATCH)
                .flatMap(operation -> operation.requestContent().stream())
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiMediaType mediaType) {
        return mediaType.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiMediaType mediaType, OpenApiContext document) {
        String type = mediaType.name().split(PARAMETERS, 2)[0].strip().toLowerCase(Locale.ROOT);
        return PATCH_DOCUMENTS.contains(type) ? Optional.empty() : Optional.of(NOT_A_PATCH);
    }
}
