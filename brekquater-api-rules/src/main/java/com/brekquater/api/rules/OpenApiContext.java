// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;
import java.util.Set;

/**
 * Read-only view of an OpenAPI document (Adapter pattern). Rules depend on this interface and never on the
 * library that parsed the document, so that library can change without touching a rule.
 *
 * <p>Every list is in a fixed order that does not depend on the order of the source document.
 */
public interface OpenApiContext {

    /** Returns every operation under {@code paths}, ordered by path and then by {@link HttpMethod} order. */
    List<ApiOperation> operations();

    /** Returns every key under {@code paths}, in order. */
    List<ApiPath> paths();

    ApiInfo info();

    /** Returns the document-wide {@code security} entries that operations inherit unless they declare their own. */
    List<ApiSecurityRequirement> rootSecurity();

    /** Returns the names of the schemes defined under {@code components.securitySchemes}. */
    Set<String> securitySchemeNames();

    DocumentSummary document();
}
