// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;
import java.util.Optional;

/**
 * One entry of the document's {@code servers} list.
 *
 * @param url the server URL as written, which may be relative
 * @param description what the document says about the server
 * @param pointer the location of the entry in the document
 */
public record ApiServer(String url, Optional<String> description, JsonPointer pointer) {

    // springdoc adds a server built from the incoming request and describes it with this text.
    private static final String GENERATED_BY_SPRINGDOC = "Generated server url";

    public ApiServer {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(pointer, "pointer");
    }

    /** Returns whether springdoc wrote this entry from the request that fetched the document. */
    public boolean generatedBySpringdoc() {
        return description.filter(GENERATED_BY_SPRINGDOC::equals).isPresent();
    }
}
