// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiMediaType;
import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Turns the {@code content} of a request body or a response into {@link ApiMediaType} views, ordered by name. */
final class MediaTypes {

    private MediaTypes() {}

    static List<ApiMediaType> of(Content content, JsonPointer pointer) {
        Map<String, MediaType> mediaTypes = Optional.<Map<String, MediaType>>ofNullable(content).orElse(Map.of());
        return new TreeMap<>(mediaTypes)
                .entrySet().stream()
                .map(entry -> new ApiMediaType(
                        entry.getKey(),
                        pointer.append(entry.getKey()),
                        Optional.ofNullable(entry.getValue())
                                .map(MediaType::getSchema)
                                .map(SchemaFacts::of)))
                .toList();
    }
}
