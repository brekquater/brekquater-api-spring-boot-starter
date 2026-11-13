// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import java.util.List;
import org.springdoc.webmvc.api.SpringDocApiVersionStrategy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Keeps the Brekquater paths working in an application that uses Spring's API versioning.
 *
 * <p>With path-segment versioning, Spring reads a version out of every request path, so a request for
 * {@code /brekquater/api/assessment} fails with 400 because "brekquater" is not a version. springdoc has the same
 * problem for its own paths and solves it by wrapping the version strategy; this class reuses springdoc's wrapper,
 * exactly as springdoc does, for the panel's base path. Requests outside that path are not affected.
 */
final class ApiVersioningExemption implements SmartInitializingSingleton {

    private final ObjectProvider<RequestMappingHandlerMapping> mappings;
    private final String basePath;

    ApiVersioningExemption(ObjectProvider<RequestMappingHandlerMapping> mappings, String basePath) {
        this.mappings = mappings;
        this.basePath = basePath;
    }

    @Override
    public void afterSingletonsInstantiated() {
        mappings.orderedStream().forEach(this::exempt);
    }

    private void exempt(RequestMappingHandlerMapping mapping) {
        ApiVersionStrategy versioning = mapping.getApiVersionStrategy();
        if (versioning != null) {
            mapping.setApiVersionStrategy(new SpringDocApiVersionStrategy(versioning, List.of(basePath)));
        }
    }
}
