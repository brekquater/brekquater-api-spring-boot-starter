// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** One operation of the assessed API, for example {@code GET /orders/{id}}. */
public interface ApiOperation {

    HttpMethod method();

    String path();

    /** Returns the location of this operation in the document, used as finding evidence. */
    JsonPointer pointer();

    Optional<String> operationId();

    Optional<String> summary();

    Optional<String> description();

    /** Returns the parameters of the operation, including those its path item declares for every operation. */
    List<ApiParameter> parameters();

    boolean hasRequestBody();

    /** Returns the media types the request body can be sent as, ordered by name; empty when there is no body. */
    List<ApiMediaType> requestContent();

    /** Returns the documented responses, ordered by status code. */
    List<ApiResponse> responses();

    /**
     * Returns the security this operation declares itself. Empty means the operation inherits the document's
     * security; an empty list means it explicitly declares none.
     */
    Optional<List<ApiSecurityRequirement>> declaredSecurity();

    /** Returns whether the operation is marked {@code x-brekquater-public: true}, meaning open on purpose. */
    boolean markedPublic();

    /** Returns the IDs of the rules this operation opts out of with {@code x-brekquater-ignore}. */
    Set<String> ignoredRuleIds();
}
