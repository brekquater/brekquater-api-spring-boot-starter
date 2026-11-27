// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.core.util.Yaml31;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Reads the committed OpenAPI document of the last release, in JSON or YAML, with the swagger-core mapper for its
 * OpenAPI version, the same one springdoc's own document is read back with.
 */
final class BaselineDocumentReader {

    private static final String OPENAPI_3_0 = "3.0";
    private static final char JSON_OBJECT = '{';

    private BaselineDocumentReader() {}

    static OpenAPI read(InputStream input) {
        try {
            String text = new String(input.readAllBytes(), StandardCharsets.UTF_8).strip();
            boolean json = !text.isEmpty() && text.charAt(0) == JSON_OBJECT;
            String version = (json ? Json.mapper() : Yaml.mapper()).readTree(text).path("openapi").asText();
            if (version.isEmpty()) {
                throw new IOException("It has no openapi field, so it is not an OpenAPI document.");
            }
            return mapper(json, version).readValue(text, OpenAPI.class);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static ObjectMapper mapper(boolean json, String version) {
        boolean openApi30 = version.startsWith(OPENAPI_3_0);
        if (json) {
            return openApi30 ? Json.mapper() : Json31.mapper();
        }
        return openApi30 ? Yaml.mapper() : Yaml31.mapper();
    }
}
