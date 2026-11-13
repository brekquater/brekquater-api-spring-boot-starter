// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/** Where the application's OpenAPI document comes from. Declare a bean of this type to supply it another way. */
public interface OpenApiDocumentSource {

    /**
     * Returns the current document, or empty when the application has none.
     *
     * @param request the request being served, which tells the source the address the application is reached at
     */
    Optional<OpenApiDocument> fetch(HttpServletRequest request);
}
