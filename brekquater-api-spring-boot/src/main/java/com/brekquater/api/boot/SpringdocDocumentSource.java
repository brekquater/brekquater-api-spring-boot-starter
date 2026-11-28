// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.api.OpenApiWebMvcResource;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.core.JacksonException;

/**
 * Reads the document springdoc already produces, in-process (Adapter pattern).
 *
 * <p>It calls springdoc's own public endpoint method, so the result is exactly what Swagger UI shows, with every
 * customizer applied. springdoc caches the document, so this costs one serialisation per call.
 *
 * <p>springdoc's other routes are worse: {@code getOpenApi} is protected, {@code OpenAPIService#build} returns a
 * document without paths, and {@code getCachedOpenAPI} returns springdoc's live object, which a rule could change.
 */
final class SpringdocDocumentSource implements OpenApiDocumentSource {

    private static final Log LOGGER = LogFactory.getLog(SpringdocDocumentSource.class);

    private final ObjectProvider<OpenApiWebMvcResource> resource;
    private final ObjectProvider<SpringDocConfigProperties> springdocProperties;

    SpringdocDocumentSource(
            ObjectProvider<OpenApiWebMvcResource> resource,
            ObjectProvider<SpringDocConfigProperties> springdocProperties) {
        this.resource = resource;
        this.springdocProperties = springdocProperties;
    }

    @Override
    public Optional<OpenApiDocument> fetch(HttpServletRequest request) {
        Optional<OpenApiWebMvcResource> available = Optional.ofNullable(resource.getIfAvailable());
        Optional<SpringDocConfigProperties> properties = Optional.ofNullable(springdocProperties.getIfAvailable());
        if (available.isEmpty() || properties.isEmpty()) {
            return Optional.empty();
        }
        String apiDocsPath = properties.get().getApiDocs().getPath();
        return read(available.get(), new ApiDocsRequest(request, apiDocsPath), apiDocsPath);
    }

    private static Optional<OpenApiDocument> read(
            OpenApiWebMvcResource resource, ApiDocsRequest request, String apiDocsPath) {
        try {
            byte[] json = resource.openapiJson(request, apiDocsPath, request.getLocale());
            return Optional.of(OpenApiDocument.of(new String(json, StandardCharsets.UTF_8)));
        } catch (JacksonException exception) {
            LOGGER.warn("springdoc could not write the OpenAPI document (" + exception.getClass().getName()
                    + "), so the API is reported as not assessed.");
            return Optional.empty();
        }
    }
}
