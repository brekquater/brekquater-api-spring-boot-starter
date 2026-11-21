// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiMediaType;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * BQ-DOC-008: a request or response body names its media type. A wildcard such as {@code *}{@code /*}, which
 * springdoc writes for every response unless told otherwise, leaves clients and code generators guessing.
 */
public final class MediaTypeIsSpecific extends OpenApiRule<ApiMediaType> {

    private static final String ANY_SUBTYPE = "/*";
    private static final Explanation WILDCARD = new Explanation(
            "Body is documented with a wildcard media type instead of the type it is sent as.",
            "Set springdoc.default-produces-media-type=application/json, or declare produces or consumes on the "
                    + "mapping, for example @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE).");

    @Override
    public String id() {
        return "BQ-DOC-008";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public Category category() {
        return Category.DOCUMENTATION;
    }

    @Override
    protected List<ApiMediaType> targets(OpenApiContext document) {
        return Stream.concat(
                        DocumentTargets.requestMediaTypes(document).stream(),
                        DocumentTargets.responseMediaTypes(document).stream())
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiMediaType mediaType) {
        return mediaType.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiMediaType mediaType, OpenApiContext document) {
        return mediaType.name().endsWith(ANY_SUBTYPE) ? Optional.of(WILDCARD) : Optional.empty();
    }
}
