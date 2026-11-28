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

    /** Returns the schemes defined under {@code components.securitySchemes}, ordered by name. */
    List<ApiSecurityScheme> securitySchemes();

    /** Returns the document-wide {@code servers} entries, in document order. */
    List<ApiServer> servers();

    /** Returns every flow of every OAuth 2.0 security scheme, ordered by scheme name. */
    List<ApiOAuthFlow> oauthFlows();

    /**
     * Returns every schema the document defines: the named schemas under {@code components}, the schemas of
     * parameters, request bodies and responses, and every schema nested in them. A {@code $ref} is listed but not
     * followed, because the schema it names is listed on its own.
     */
    List<ApiSchemaNode> schemas();

    /**
     * Returns every example and default in the document, each already validated against its schema: those of
     * schemas first, then those of parameters and JSON bodies. A schema's {@code format} is not enforced, because
     * JSON Schema treats it as a note.
     */
    List<ApiExample> examples();

    DocumentSummary document();
}
