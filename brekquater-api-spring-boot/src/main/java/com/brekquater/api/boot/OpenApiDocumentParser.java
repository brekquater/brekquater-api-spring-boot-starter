// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads springdoc's JSON back into a private swagger-models object, with the same swagger-core mapper springdoc
 * wrote it with. Rules therefore work on a copy and can never change the document Swagger UI shows.
 */
final class OpenApiDocumentParser {

    private static final Log LOGGER = LogFactory.getLog(OpenApiDocumentParser.class);
    private static final String OPENAPI_3_0 = "3.0";

    private OpenApiDocumentParser() {}

    static Optional<OpenAPI> parse(OpenApiDocument document) {
        try {
            return Optional.ofNullable(mapperFor(document.json()).readValue(document.json(), OpenAPI.class));
        } catch (JacksonException exception) {
            LOGGER.warn("The OpenAPI document could not be read (" + exception.getClass().getName()
                    + "), so the API is reported as not assessed.");
            return Optional.empty();
        }
    }

    // OpenAPI 3.0 and 3.1 schemas differ, and swagger-core has one mapper for each.
    private static ObjectMapper mapperFor(String json) {
        String version = Json.mapper().readTree(json).path("openapi").asString();
        return version.startsWith(OPENAPI_3_0) ? Json.mapper() : Json31.mapper();
    }
}
