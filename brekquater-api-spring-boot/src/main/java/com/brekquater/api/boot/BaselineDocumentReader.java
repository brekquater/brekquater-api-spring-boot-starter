// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.evidence.UnreadableReportException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Json31;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.snakeyaml.engine.v2.api.LoadSettings;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Reads the committed OpenAPI document of the last release, in JSON or YAML, into the swagger-core model springdoc's
 * own document is read back into. Every way the file can be wrong is reported with a reason, and the reason never
 * quotes the file.
 */
final class BaselineDocumentReader {

    private static final String OPENAPI_3_0 = "3.0";
    private static final char JSON_OBJECT = '{';
    private static final char BYTE_ORDER_MARK = '﻿';
    private static final int UTF_16_MARK = 0xFE;
    // SnakeYAML stops at 3 million characters by default; allow as much as any report may be.
    private static final ObjectMapper YAML = new YAMLMapper(YAMLFactory.builder()
            .loadSettings(loadSettings())
            .build());

    private BaselineDocumentReader() {}

    static OpenAPI read(InputStream input) {
        JsonNode tree = tree(text(bytes(input)));
        String version = tree.path("openapi").asString();
        if (version.isEmpty()) {
            throw new UnreadableReportException("It has no openapi field, so it is not an OpenAPI document.");
        }
        try {
            return (version.startsWith(OPENAPI_3_0) ? Json.mapper() : Json31.mapper()).treeToValue(tree, OpenAPI.class);
        } catch (JacksonException malformed) {
            throw new UnreadableReportException("It is JSON or YAML, but not a readable OpenAPI document.");
        }
    }

    private static byte[] bytes(InputStream input) {
        try {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String text(byte[] bytes) {
        if (utf16(bytes)) {
            throw new UnreadableReportException("It is saved as UTF-16; save it as UTF-8, for example with curl -o.");
        }
        String text = new String(bytes, StandardCharsets.UTF_8).strip();
        return !text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK ? text.substring(1) : text;
    }

    // Windows PowerShell's Out-File writes UTF-16 with the mark FF FE. Without a mark, the zero byte after an
    // ASCII character gives it away, because an OpenAPI document starts with ASCII.
    private static boolean utf16(byte[] bytes) {
        if (bytes.length < 2) {
            return false;
        }
        boolean marked = (bytes[0] & 0xFF) >= UTF_16_MARK && (bytes[1] & 0xFF) >= UTF_16_MARK;
        return marked || bytes[0] == 0 || bytes[1] == 0;
    }

    private static JsonNode tree(String text) {
        boolean json = !text.isEmpty() && text.charAt(0) == JSON_OBJECT;
        try {
            return (json ? Json.mapper() : YAML).readTree(text);
        } catch (JacksonException malformed) {
            throw new UnreadableReportException("It is not valid " + (json ? "JSON." : "YAML."));
        }
    }

    private static LoadSettings loadSettings() {
        return LoadSettings.builder()
                .setCodePointLimit((int) BuildReportFiles.MAXIMUM_BYTES)
                .build();
    }
}
