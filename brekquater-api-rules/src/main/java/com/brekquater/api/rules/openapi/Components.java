// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves {@code $ref} references to the document's {@code components}. Only one level is followed: components
 * that refer to further components are rare in generated documents, and stopping there makes cycles impossible.
 */
final class Components {

    private static final String PARAMETERS = "#/components/parameters/";
    private static final String RESPONSES = "#/components/responses/";

    private final Map<String, Parameter> parameters;
    private final Map<String, ApiResponse> responses;

    private Components(Map<String, Parameter> parameters, Map<String, ApiResponse> responses) {
        this.parameters = parameters;
        this.responses = responses;
    }

    static Components of(OpenAPI openApi) {
        Optional<io.swagger.v3.oas.models.Components> components = Optional.ofNullable(openApi.getComponents());
        return new Components(
                components.map(io.swagger.v3.oas.models.Components::getParameters).orElse(Map.of()),
                components.map(io.swagger.v3.oas.models.Components::getResponses).orElse(Map.of()));
    }

    Parameter resolve(Parameter parameter) {
        return named(parameter.get$ref(), PARAMETERS, parameters).orElse(parameter);
    }

    ApiResponse resolve(ApiResponse response) {
        return named(response.get$ref(), RESPONSES, responses).orElse(response);
    }

    private static <T> Optional<T> named(String reference, String prefix, Map<String, T> definitions) {
        return Optional.ofNullable(reference)
                .filter(ref -> ref.startsWith(prefix))
                .map(ref -> definitions.get(ref.substring(prefix.length())));
    }
}
