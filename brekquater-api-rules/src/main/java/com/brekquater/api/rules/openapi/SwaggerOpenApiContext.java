// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiExample;
import com.brekquater.api.rules.ApiInfo;
import com.brekquater.api.rules.ApiOAuthFlow;
import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.ApiPath;
import com.brekquater.api.rules.ApiSchemaNode;
import com.brekquater.api.rules.ApiSecurityRequirement;
import com.brekquater.api.rules.ApiSecurityScheme;
import com.brekquater.api.rules.ApiServer;
import com.brekquater.api.rules.DocumentSummary;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import java.util.Collections;
import java.util.List;
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
    private final List<ApiSecurityScheme> securitySchemes;
    private final List<ApiServer> servers;
    private final List<ApiOAuthFlow> oauthFlows;
    private final List<ApiSchemaNode> schemas;
    private final List<ApiExample> examples;
    private final DocumentSummary document;

    public SwaggerOpenApiContext(OpenAPI openApi) {
        this.operations = OperationIndex.of(openApi);
        this.paths = OperationIndex.sortedPaths(openApi).keySet().stream()
                .map(path -> new ApiPath(path, PATHS.append(path)))
                .toList();
        this.info = infoOf(openApi);
        this.rootSecurity = SecurityRequirements.of(
                Optional.ofNullable(openApi.getSecurity()).orElse(List.of()), SECURITY);
        this.securitySchemes = SecuritySchemes.of(openApi);
        this.servers = Servers.of(openApi);
        this.oauthFlows = OAuthFlowIndex.of(openApi);
        List<SchemaIndex.Visit> visits = SchemaIndex.of(openApi);
        this.schemas = SchemaIndex.nodes(visits, new LocalRefs(openApi));
        this.examples = ExampleIndex.of(openApi, visits);
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
        Set<String> names = new TreeSet<>();
        securitySchemes.forEach(scheme -> names.add(scheme.name()));
        return Collections.unmodifiableSet(names);
    }

    @Override
    public List<ApiSecurityScheme> securitySchemes() {
        return securitySchemes;
    }

    @Override
    public List<ApiServer> servers() {
        return servers;
    }

    @Override
    public List<ApiOAuthFlow> oauthFlows() {
        return oauthFlows;
    }

    @Override
    public List<ApiSchemaNode> schemas() {
        return schemas;
    }

    @Override
    public List<ApiExample> examples() {
        return examples;
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
}
