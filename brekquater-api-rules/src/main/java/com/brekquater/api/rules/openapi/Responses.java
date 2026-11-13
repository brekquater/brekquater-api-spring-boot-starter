// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiMediaType;
import com.brekquater.api.rules.ApiResponse;
import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/** Turns the {@code responses} of a swagger-models operation into {@link ApiResponse} views, in code order. */
final class Responses {

    private static final String RESPONSES = "responses";
    private static final String CONTENT = "content";

    private Responses() {}

    static List<ApiResponse> of(ApiResponses responses, JsonPointer operation, Components components) {
        Map<String, io.swagger.v3.oas.models.responses.ApiResponse> declared =
                Optional.<Map<String, io.swagger.v3.oas.models.responses.ApiResponse>>ofNullable(responses)
                        .orElse(Map.of());
        JsonPointer pointer = operation.append(RESPONSES);
        return new TreeMap<>(declared)
                .entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()))
                .map(entry -> response(entry.getKey(), components.resolve(entry.getValue()), pointer))
                .toList();
    }

    private static ApiResponse response(
            String code, io.swagger.v3.oas.models.responses.ApiResponse response, JsonPointer responses) {
        JsonPointer pointer = responses.append(code);
        return new ApiResponse(code, pointer, content(response, pointer.append(CONTENT)));
    }

    private static List<ApiMediaType> content(
            io.swagger.v3.oas.models.responses.ApiResponse response, JsonPointer content) {
        Map<String, MediaType> mediaTypes = Optional.<Map<String, MediaType>>ofNullable(response.getContent())
                .orElse(Map.of());
        return new TreeMap<>(mediaTypes)
                .entrySet().stream()
                .map(entry -> new ApiMediaType(
                        entry.getKey(),
                        content.append(entry.getKey()),
                        Optional.ofNullable(entry.getValue())
                                .map(MediaType::getSchema)
                                .map(SchemaFacts::of)))
                .toList();
    }
}
