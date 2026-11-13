// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Calls the running demo over real HTTP, the way a browser or Swagger UI would. */
final class DemoHttpClient {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient client = HttpClient.newHttpClient();
    private final int port;

    DemoHttpClient(int port) {
        this.port = port;
    }

    HttpResponse<String> get(String path) {
        URI uri = URI.create("http://localhost:" + port + path);
        try {
            return client.send(HttpRequest.newBuilder(uri).build(), BodyHandlers.ofString());
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling " + uri, exception);
        }
    }

    JsonNode assessment() {
        return JSON.readTree(get("/brekquater/api/assessment").body());
    }
}
