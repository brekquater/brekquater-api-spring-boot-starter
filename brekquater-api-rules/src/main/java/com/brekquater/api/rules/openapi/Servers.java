// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.openapi;

import com.brekquater.api.rules.ApiServer;
import com.brekquater.api.rules.JsonPointer;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Reads the document-wide {@code servers} list, in document order. Servers declared on a path or an operation are
 * rare in generated documents and are not read.
 */
final class Servers {

    private static final JsonPointer SERVERS = JsonPointer.root().append("servers");

    private Servers() {}

    static List<ApiServer> of(OpenAPI openApi) {
        List<Server> servers = Optional.ofNullable(openApi.getServers()).orElse(List.of());
        return IntStream.range(0, servers.size())
                .filter(index -> servers.get(index) != null && servers.get(index).getUrl() != null)
                .mapToObj(index -> new ApiServer(
                        servers.get(index).getUrl(),
                        Optional.ofNullable(servers.get(index).getDescription()),
                        SERVERS.append(String.valueOf(index))))
                .toList();
    }
}
