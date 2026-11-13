// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiMediaType;
import com.brekquater.api.rules.ApiOperation;
import com.brekquater.api.rules.ApiResponse;
import com.brekquater.api.rules.ApiSecurityRequirement;
import com.brekquater.api.rules.OpenApiContext;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Target lists that several rules share, so each is defined once. */
final class DocumentTargets {

    private DocumentTargets() {}

    static List<ApiResponse> responses(OpenApiContext document) {
        return document.operations().stream().flatMap(operation -> operation.responses().stream()).toList();
    }

    static List<ApiMediaType> responseMediaTypes(OpenApiContext document) {
        return responses(document).stream().flatMap(response -> response.content().stream()).toList();
    }

    static List<ApiOperation> operationsWithId(OpenApiContext document) {
        return document.operations().stream()
                .filter(operation -> operation.operationId().filter(Predicate.not(String::isBlank)).isPresent())
                .toList();
    }

    /** Returns the document-wide security entries followed by those every operation declares itself. */
    static List<ApiSecurityRequirement> securityRequirements(OpenApiContext document) {
        Stream<ApiSecurityRequirement> declared = document.operations().stream()
                .flatMap(operation -> operation.declaredSecurity().orElse(List.of()).stream());
        return Stream.concat(document.rootSecurity().stream(), declared).toList();
    }
}
