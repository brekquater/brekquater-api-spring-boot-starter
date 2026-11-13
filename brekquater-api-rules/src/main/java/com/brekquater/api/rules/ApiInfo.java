// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;
import java.util.Optional;

/**
 * The {@code info} object of the document.
 *
 * @param pointer the location of {@code info}, used as finding evidence
 * @param title the API title
 * @param version the API version
 * @param description the API description
 * @param hasContact whether a contact gives at least one non-blank name, URL or email
 */
public record ApiInfo(
        JsonPointer pointer,
        Optional<String> title,
        Optional<String> version,
        Optional<String> description,
        boolean hasContact) {

    public ApiInfo {
        Objects.requireNonNull(pointer, "pointer");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(description, "description");
    }
}
