// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiSchemaNode;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * BQ-SEC-017: no password or secret can be read in a response. A property named like one, in a schema a response
 * returns, must be {@code writeOnly}: accepted in requests, never sent back. Tokens are not checked, because login
 * and token endpoints return them on purpose.
 *
 * <p>The name list is a heuristic, which would normally make this an INFO rule. It is MAJOR because a false
 * positive is rare and cheap to fix, while a returned password is a breach.
 */
public final class SecretIsNotReturned extends MajorSecurityRule<ApiSchemaNode> {

    private static final Set<String> SECRETS = Set.of("password", "passwd", "pwd", "secret", "clientsecret");
    private static final Explanation READABLE_SECRET = new Explanation(
            "A password or secret is part of a response, so the API sends it back to callers.",
            "Annotate the field with @JsonProperty(access = JsonProperty.Access.WRITE_ONLY): Jackson then leaves it "
                    + "out of responses and springdoc marks it writeOnly. @Schema(accessMode = WRITE_ONLY) alone only "
                    + "changes the documentation.");

    @Override
    public String id() {
        return "BQ-SEC-017";
    }

    @Override
    protected List<ApiSchemaNode> targets(OpenApiContext document) {
        return ResponseSchemas.in(document).stream()
                .filter(schema -> schema.propertyName().filter(SecretIsNotReturned::namesSecret).isPresent())
                .toList();
    }

    @Override
    protected JsonPointer locate(ApiSchemaNode property) {
        return property.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiSchemaNode property, OpenApiContext document) {
        return property.writeOnly() ? Optional.empty() : Optional.of(READABLE_SECRET);
    }

    private static boolean namesSecret(String name) {
        return SECRETS.contains(name.toLowerCase(Locale.ROOT).replace("_", "").replace("-", ""));
    }
}
