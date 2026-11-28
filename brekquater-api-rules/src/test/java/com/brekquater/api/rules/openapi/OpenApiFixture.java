// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.AssessmentContext;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Turns an inline YAML fixture into what a rule sees. It reads the YAML with swagger-core, the library springdoc
 * uses, so a fixture reaches the rules in the same shape as a real application's document.
 */
public final class OpenApiFixture {

    private static final String OPENAPI_3_0 = "openapi: 3.0";

    private OpenApiFixture() {}

    public static AssessmentContext context(String yaml) {
        return AssessmentContext.of(new SwaggerOpenApiContext(parse(yaml)));
    }

    public static OpenAPI parse(String yaml) {
        ObjectMapper mapper = yaml.startsWith(OPENAPI_3_0) ? Yaml.mapper() : Yaml31.mapper();
        try {
            return mapper.readValue(yaml, OpenAPI.class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("The fixture is not a readable OpenAPI document", exception);
        }
    }
}
