// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;

/**
 * Identifying facts about the assessed OpenAPI document. A value the document does not state is an empty string.
 *
 * @param openApiVersion the specification version the document declares, for example {@code 3.1.0}
 * @param title the API title from {@code info}
 * @param version the API version from {@code info}
 * @param operationCount how many operations the document declares
 */
public record DocumentSummary(String openApiVersion, String title, String version, int operationCount) {

    public DocumentSummary {
        Objects.requireNonNull(openApiVersion, "openApiVersion");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(version, "version");
    }
}
