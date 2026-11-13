// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo.bad;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Simulates hand edits to the generated document. springdoc always writes an operationId and a schema, so these
 * two mistakes only appear when a customizer or a hand-written spec makes them.
 *
 * <p>A duplicate operationId (BQ-API-004) cannot be shown this way: springdoc's own {@code OperationIdCustomizer}
 * runs after this one and renames duplicates, which is what produces BQ-API-002 instead.
 */
@Profile("bad")
@Component
class HandEditedDocument implements OpenApiCustomizer {

    @Override
    public void customise(OpenAPI openApi) {
        Operation orderItem = openApi.getPaths().get("/orderItems/{id}").getGet();
        Operation search = openApi.getPaths().get("/orders/search").getGet();

        // BQ-API-001: an operation without an operationId.
        orderItem.setOperationId(null);
        // BQ-API-003: a response body without a schema.
        search.getResponses().addApiResponse("200", new ApiResponse()
                .description("Matching orders")
                .content(new Content().addMediaType("application/json", new MediaType())));
    }
}
