// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiSchemaNode;
import com.brekquater.api.rules.OpenApiContext;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Finds the schemas a client can receive: those written inline in a response, and every named schema a response
 * refers to, directly or through other named schemas.
 */
final class ResponseSchemas {

    private static final String PATHS = "paths";
    private static final String RESPONSES = "responses";
    private static final String COMPONENTS = "components";
    private static final String SCHEMAS = "schemas";

    private ResponseSchemas() {}

    static List<ApiSchemaNode> in(OpenApiContext document) {
        Set<String> reachable = reachableNames(document);
        return document.schemas().stream()
                .filter(schema -> inResponse(schema) || namedSchema(schema).filter(reachable::contains).isPresent())
                .toList();
    }

    private static Set<String> reachableNames(OpenApiContext document) {
        Set<String> reachable = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        document.schemas().stream().filter(ResponseSchemas::inResponse)
                .forEach(schema -> schema.referencedSchemaName().ifPresent(pending::add));
        while (!pending.isEmpty()) {
            String name = pending.pop();
            if (reachable.add(name)) {
                document.schemas().stream()
                        .filter(schema -> namedSchema(schema).filter(name::equals).isPresent())
                        .forEach(schema -> schema.referencedSchemaName().ifPresent(pending::add));
            }
        }
        return reachable;
    }

    // A response schema sits at /paths/{path}/{method}/responses/...
    private static boolean inResponse(ApiSchemaNode schema) {
        List<String> tokens = schema.pointer().tokens();
        return tokens.size() > 3 && tokens.get(0).equals(PATHS) && tokens.get(3).equals(RESPONSES);
    }

    // A named schema, or anything inside one, sits at /components/schemas/{name}/...
    private static Optional<String> namedSchema(ApiSchemaNode schema) {
        List<String> tokens = schema.pointer().tokens();
        boolean named = tokens.size() > 2 && tokens.get(0).equals(COMPONENTS) && tokens.get(1).equals(SCHEMAS);
        return named ? Optional.of(tokens.get(2)) : Optional.empty();
    }
}
