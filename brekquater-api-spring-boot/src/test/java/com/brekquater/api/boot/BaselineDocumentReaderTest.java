// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.brekquater.api.rules.evidence.UnreadableReportException;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class BaselineDocumentReaderTest {

    private static final String YAML = """
            openapi: 3.1.0
            info: {title: Orders API, version: 1.0.0}
            paths: {}
            """;

    private static OpenAPI read(byte[] bytes) {
        return BaselineDocumentReader.read(new ByteArrayInputStream(bytes));
    }

    private static OpenAPI read(String text) {
        return read(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readsJsonAndYamlWithOrWithoutAByteOrderMark() {
        assertThat(read("{\"openapi\": \"3.0.1\", \"info\": {\"title\": \"Orders API\", \"version\": \"1.0.0\"}}")
                .getInfo().getVersion()).isEqualTo("1.0.0");
        assertThat(read("﻿" + YAML).getOpenapi()).isEqualTo("3.1.0");
    }

    @Test
    void readsAYamlDocumentLargerThanSnakeYamlsDefaultLimit() {
        String large = YAML + "x-notes: '" + "a".repeat(3_500_000) + "'\n";

        assertThat(read(large).getOpenapi()).isEqualTo("3.1.0");
    }

    @Test
    void saysWhyAFileIsNotABaseline() {
        assertThatThrownBy(() -> read(YAML.getBytes(StandardCharsets.UTF_16)))
                .isInstanceOf(UnreadableReportException.class).hasMessageContaining("UTF-16");
        assertThatThrownBy(() -> read(YAML.getBytes(StandardCharsets.UTF_16LE)))
                .isInstanceOf(UnreadableReportException.class).hasMessageContaining("UTF-16");
        assertThatThrownBy(() -> read("orders: [1, 2"))
                .isInstanceOf(UnreadableReportException.class).hasMessage("It is not valid YAML.");
        assertThatThrownBy(() -> read("{\"orders\": []}"))
                .isInstanceOf(UnreadableReportException.class).hasMessageContaining("no openapi field");
    }
}
