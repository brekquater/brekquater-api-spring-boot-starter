// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * The one walk over {@code paths} that every rule shares. Paths are sorted because generated documents do not
 * keep a stable order between runs.
 */
final class OperationIndex {

    private OperationIndex() {}

    static List<ApiOperation> of(OpenAPI openApi) {
        Components components = Components.of(openApi);
        return sortedPaths(openApi)
                .entrySet().stream()
                .flatMap(path -> sourcesOf(path.getKey(), path.getValue()))
                .map(source -> (ApiOperation) new SwaggerOperation(source, components))
                .toList();
    }

    static TreeMap<String, PathItem> sortedPaths(OpenAPI openApi) {
        Map<String, PathItem> paths = Optional.<Map<String, PathItem>>ofNullable(openApi.getPaths()).orElse(Map.of());
        TreeMap<String, PathItem> sorted = new TreeMap<>();
        paths.forEach((path, item) -> {
            if (Objects.nonNull(item)) {
                sorted.put(path, item);
            }
        });
        return sorted;
    }

    private static Stream<OperationSource> sourcesOf(String path, PathItem pathItem) {
        Map<PathItem.HttpMethod, Operation> declared = pathItem.readOperationsMap();
        List<Parameter> shared = Optional.ofNullable(pathItem.getParameters()).orElse(List.of());
        return Arrays.stream(HttpMethod.values())
                .filter(method -> declared.containsKey(swaggerMethod(method)))
                .map(method -> new OperationSource(path, method, declared.get(swaggerMethod(method)), shared));
    }

    // Both enums use the HTTP method names, so the name is the mapping.
    private static PathItem.HttpMethod swaggerMethod(HttpMethod method) {
        return PathItem.HttpMethod.valueOf(method.name());
    }
}
