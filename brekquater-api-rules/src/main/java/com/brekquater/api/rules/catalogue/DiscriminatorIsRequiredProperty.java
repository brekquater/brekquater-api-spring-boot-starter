// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiSchemaNode;
import com.brekquater.api.rules.ApiSchemaProperties;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/**
 * BQ-DES-013: the property a {@code discriminator} names is defined and required, in the schema itself or in
 * every branch. Without it, a client cannot tell which variant it received.
 */
public final class DiscriminatorIsRequiredProperty extends OpenApiRule<ApiSchemaNode> {

    @Override
    public String id() {
        return "BQ-DES-013";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public Category category() {
        return Category.DESIGN;
    }

    @Override
    protected List<ApiSchemaNode> targets(OpenApiContext document) {
        return document.schemas().stream()
                .filter(schema -> schema.shape().properties().discriminator().isPresent())
                .filter(schema -> schema.shape().properties().complete())
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiSchemaNode target) {
        return target.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiSchemaNode target, OpenApiContext document) {
        ApiSchemaProperties properties = target.shape().properties();
        String name = properties.discriminator().orElseThrow();
        if (properties.defined().contains(name) && properties.alwaysRequired().contains(name)) {
            return Optional.empty();
        }
        return Optional.of(new Explanation(
                "Discriminator property " + name + " is not defined and required for every variant.",
                "Declare the property in the base schema or in every subtype, and list it as required."));
    }
}
