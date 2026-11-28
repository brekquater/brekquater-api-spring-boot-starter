// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The properties of an object schema, with {@code allOf}, {@code anyOf}, {@code oneOf} and local {@code $ref}
 * already followed.
 *
 * @param defined every property the schema or any of its branches defines
 * @param alwaysRequired the names an object must have: required here, in any {@code allOf} branch, or in every
 *     {@code anyOf} or {@code oneOf} branch
 * @param complete whether every branch could be read; false when a {@code $ref} leads outside the document
 * @param discriminator the {@code discriminator.propertyName}, if the schema declares one
 */
public record ApiSchemaProperties(
        Set<String> defined,
        Set<String> alwaysRequired,
        boolean complete,
        Optional<String> discriminator) {

    public ApiSchemaProperties {
        defined = Set.copyOf(defined);
        alwaysRequired = Set.copyOf(alwaysRequired);
        Objects.requireNonNull(discriminator, "discriminator");
    }
}
