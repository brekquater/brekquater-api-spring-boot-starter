// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

/**
 * Adds the Brekquater panel to Swagger UI through springdoc's supported hook, {@code SwaggerIndexTransformer}.
 *
 * <p>springdoc's own transformation runs first and unchanged, so OAuth, CSRF and configuration settings keep
 * working. Then a short loader is appended to {@code swagger-initializer.js}. An application that defines its own
 * {@code SwaggerIndexTransformer} keeps it: this bean backs off, and the standalone page still shows the panel.
 */
final class PanelInjectingIndexTransformer extends SwaggerIndexPageTransformer {

    private static final Log LOGGER = LogFactory.getLog(PanelInjectingIndexTransformer.class);
    private static final String INITIALIZER = "swagger-initializer.js";

    private final String basePath;

    PanelInjectingIndexTransformer(SwaggerUiCollaborators springdoc, String basePath) {
        super(springdoc.config(), springdoc.oAuth(), springdoc.welcome(), springdoc.objectMapperProvider());
        this.basePath = basePath;
    }

    @Override
    public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain chain)
            throws IOException {
        Resource transformed = super.transform(request, resource, chain);
        if (!INITIALIZER.equals(resource.getFilename())) {
            return transformed;
        }
        return loaderFor(request.getContextPath() + basePath)
                .map(loader -> appended(transformed, resource, loader))
                .orElse(transformed);
    }

    private static Optional<String> loaderFor(String panelPath) {
        Optional<String> loader = PanelLoaderScript.forPanelAt(panelPath);
        if (loader.isEmpty()) {
            LOGGER.warn("The Brekquater panel is not added to Swagger UI because its path contains characters that "
                    + "cannot be written into a script. The standalone page still works.");
        }
        return loader;
    }

    private static Resource appended(Resource transformed, Resource original, String loader) {
        try {
            String script = transformed.getContentAsString(StandardCharsets.UTF_8) + loader;
            return new TransformedResource(original, script.getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /** The springdoc beans its own transformer is built from, grouped so the constructor stays small. */
    record SwaggerUiCollaborators(
            SwaggerUiConfigProperties config,
            SwaggerUiOAuthProperties oAuth,
            SwaggerWelcomeCommon welcome,
            ObjectMapperProvider objectMapperProvider) {}
}
