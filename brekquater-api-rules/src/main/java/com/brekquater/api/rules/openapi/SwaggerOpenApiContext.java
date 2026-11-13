// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiInfo;
import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.ApiPath;
import com.brekquater.api.rules.ApiSecurityRequirement;
import com.brekquater.api.rules.DocumentSummary;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * {@link OpenApiContext} backed by a swagger-models {@link OpenAPI} (Adapter pattern). This package is the only
 * place that touches swagger-models; an architecture test keeps it that way.
 *
 * <p>The document is read once, at construction, and never modified.
 */
public final class SwaggerOpenApiContext implements OpenApiContext {

    private static final String NOT_STATED = "";
    private static final JsonPointer INFO = JsonPointer.root().append("info");
    private static final JsonPointer PATHS = JsonPointer.root().append("paths");
    private static final JsonPointer SECURITY = JsonPointer.root().append("security");

    private final List<ApiOperation> operations;
    private final List<ApiPath> paths;
    private final ApiInfo info;
    private final List<ApiSecurityRequirement> rootSecurity;
    private final Set<String> securitySchemeNames;
    private final DocumentSummary document;

    public SwaggerOpenApiContext(OpenAPI openApi) {
        this.operations = OperationIndex.of(openApi);
        this.paths = OperationIndex.sortedPaths(openApi).keySet().stream()
                .map(path -> new ApiPath(path, PATHS.append(path)))
                .toList();
        this.info = infoOf(openApi);
        this.rootSecurity = SecurityRequirements.of(
                Optional.ofNullable(openApi.getSecurity()).orElse(List.of()), SECURITY);
        this.securitySchemeNames = securitySchemeNamesOf(openApi);
        this.document = new DocumentSummary(
                Optional.ofNullable(openApi.getOpenapi()).orElse(NOT_STATED),
                info.title().orElse(NOT_STATED),
                info.version().orElse(NOT_STATED),
                operations.size());
    }

    @Override
    public List<ApiOperation> operations() {
        return operations;
    }

    @Override
    public List<ApiPath> paths() {
        return paths;
    }

    @Override
    public ApiInfo info() {
        return info;
    }

    @Override
    public List<ApiSecurityRequirement> rootSecurity() {
        return rootSecurity;
    }

    @Override
    public Set<String> securitySchemeNames() {
        return securitySchemeNames;
    }

    @Override
    public DocumentSummary document() {
        return document;
    }

    private static ApiInfo infoOf(OpenAPI openApi) {
        Optional<Info> info = Optional.ofNullable(openApi.getInfo());
        return new ApiInfo(
                INFO,
                info.map(Info::getTitle),
                info.map(Info::getVersion),
                info.map(Info::getDescription),
                info.map(Info::getContact).filter(SwaggerOpenApiContext::reachable).isPresent());
    }

    private static boolean reachable(Contact contact) {
        return Stream.of(contact.getName(), contact.getUrl(), contact.getEmail())
                .anyMatch(value -> value != null && !value.isBlank());
    }

    private static Set<String> securitySchemeNamesOf(OpenAPI openApi) {
        Map<String, SecurityScheme> schemes = Optional.ofNullable(openApi.getComponents())
                .map(io.swagger.v3.oas.models.Components::getSecuritySchemes)
                .orElse(Map.of());
        Set<String> names = new TreeSet<>();
        schemes.forEach((name, scheme) -> {
            if (scheme != null) {
                names.add(name);
            }
        });
        return Collections.unmodifiableSet(names);
    }
}
