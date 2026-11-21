// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiPath;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * BQ-SEC-011: the document lists no Actuator endpoint other than {@code health} and {@code info}. The others
 * reveal configuration, memory, threads or loggers, or change the running application, and an API document is an
 * invitation to call them. Spring Boot exposes only {@code health} over HTTP by default.
 */
public final class NoSensitiveActuatorEndpoint extends OpenApiRule<ApiPath> {

    private static final String ACTUATOR = "actuator";
    private static final Set<String> HARMLESS = Set.of("health", "info");

    @Override
    public String id() {
        return "BQ-SEC-011";
    }

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    public Category category() {
        return Category.SECURITY;
    }

    @Override
    protected List<ApiPath> targets(OpenApiContext document) {
        return document.paths();
    }

    @Override
    protected JsonPointer locate(ApiPath path) {
        return path.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiPath path, OpenApiContext document) {
        List<String> segments = path.literalSegments();
        int actuator = segments.indexOf(ACTUATOR);
        if (actuator < 0 || actuator + 1 >= segments.size()) {
            return Optional.empty();
        }
        String endpoint = segments.get(actuator + 1);
        return HARMLESS.contains(endpoint) ? Optional.empty() : Optional.of(exposed(endpoint));
    }

    private static Explanation exposed(String endpoint) {
        return new Explanation(
                "Path exposes the Actuator endpoint '" + endpoint + "'.",
                "Remove it from management.endpoints.web.exposure.include, or serve Actuator on its own port with "
                        + "management.server.port, and keep springdoc.show-actuator=false.");
    }
}
