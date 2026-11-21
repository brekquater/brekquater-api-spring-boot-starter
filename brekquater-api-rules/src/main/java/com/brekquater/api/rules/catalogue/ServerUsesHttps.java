// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiServer;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import java.util.List;
import java.util.Optional;

/**
 * BQ-SEC-006: a declared server is reached over {@code https}. Over plain {@code http}, credentials and data can be
 * read and changed on the way. Loopback hosts are exempt, and so is the entry springdoc builds from the request that
 * fetched the document, which says how you reached it, not where the API is served.
 */
public final class ServerUsesHttps extends MajorSecurityRule<ApiServer> {

    private static final Explanation PLAIN_HTTP = new Explanation(
            "Server is reached over plain http, so credentials and data travel unencrypted.",
            "Use the https URL in @Server(url = ...) or in the servers of your OpenAPI bean, and serve the API "
                    + "behind TLS.");

    @Override
    public String id() {
        return "BQ-SEC-006";
    }

    @Override
    protected List<ApiServer> targets(OpenApiContext document) {
        return document.servers().stream().filter(server -> !server.generatedBySpringdoc()).toList();
    }

    @Override
    protected JsonPointer locate(ApiServer server) {
        return server.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiServer server, OpenApiContext document) {
        return PlainHttpUrl.reachesAnotherHost(server.url()) ? Optional.of(PLAIN_HTTP) : Optional.empty();
    }
}
