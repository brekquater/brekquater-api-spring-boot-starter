// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiMediaType;
import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.ApiParameter;
import com.brekquater.api.rules.ApiResponse;
import com.brekquater.api.rules.ApiSecurityRequirement;
import com.brekquater.api.rules.HttpMethod;
import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** {@link ApiOperation} backed by a swagger-models {@link Operation} (Adapter pattern). */
final class SwaggerOperation implements ApiOperation {

    private static final String PATHS = "paths";
    private static final String SECURITY = "security";
    private static final String REQUEST_BODY = "requestBody";
    private static final String CONTENT = "content";
    private static final String IGNORE_EXTENSION = "x-brekquater-ignore";
    private static final String PUBLIC_EXTENSION = "x-brekquater-public";

    private final HttpMethod method;
    private final String path;
    private final Operation operation;
    private final List<Parameter> pathItemParameters;
    private final Components components;

    SwaggerOperation(OperationSource source, Components components) {
        this.method = source.method();
        this.path = source.path();
        this.operation = source.operation();
        this.pathItemParameters = source.pathItemParameters();
        this.components = components;
    }

    @Override
    public HttpMethod method() {
        return method;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public JsonPointer pointer() {
        return JsonPointer.root().append(PATHS).append(path).append(method.pathItemKey());
    }

    @Override
    public Optional<String> operationId() {
        return Optional.ofNullable(operation.getOperationId());
    }

    @Override
    public Optional<String> summary() {
        return Optional.ofNullable(operation.getSummary());
    }

    @Override
    public Optional<String> description() {
        return Optional.ofNullable(operation.getDescription());
    }

    @Override
    public List<ApiParameter> parameters() {
        List<Parameter> own = Optional.ofNullable(operation.getParameters()).orElse(List.of());
        return Stream.concat(pathItemParameters.stream(), own.stream())
                .filter(Objects::nonNull)
                .map(components::resolve)
                .filter(parameter -> parameter.getName() != null && parameter.getIn() != null)
                .map(parameter -> new ApiParameter(parameter.getName(), parameter.getIn()))
                .toList();
    }

    @Override
    public boolean hasRequestBody() {
        return operation.getRequestBody() != null;
    }

    @Override
    public List<ApiMediaType> requestContent() {
        return Optional.ofNullable(operation.getRequestBody())
                .map(body -> MediaTypes.of(body.getContent(), pointer().append(REQUEST_BODY).append(CONTENT)))
                .orElse(List.of());
    }

    @Override
    public List<ApiResponse> responses() {
        return Responses.of(operation.getResponses(), pointer(), components);
    }

    @Override
    public Optional<List<ApiSecurityRequirement>> declaredSecurity() {
        return Optional.ofNullable(operation.getSecurity())
                .map(requirements -> SecurityRequirements.of(requirements, pointer().append(SECURITY)));
    }

    @Override
    public boolean markedPublic() {
        Object marker = extensions().get(PUBLIC_EXTENSION);
        // springdoc writes a boolean when the extension is declared with parseValue = true, and a string otherwise.
        return Boolean.TRUE.equals(marker) || "true".equalsIgnoreCase(String.valueOf(marker));
    }

    @Override
    public Set<String> ignoredRuleIds() {
        Object declared = extensions().getOrDefault(IGNORE_EXTENSION, Set.of());
        // Anything that is not a list, such as a single string, is a malformed opt-out and ignores nothing.
        return declared instanceof Collection<?> ruleIds ? sorted(ruleIds) : Set.of();
    }

    private Map<String, Object> extensions() {
        return Optional.ofNullable(operation.getExtensions()).orElse(Map.of());
    }

    private static Set<String> sorted(Collection<?> values) {
        Set<String> sorted = values.stream().map(String::valueOf).collect(Collectors.toCollection(TreeSet::new));
        return Collections.unmodifiableSet(sorted);
    }
}
