// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiPath;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * BQ-DES-006: no two paths differ only in the names of their path variables. OpenAPI forbids such paths, because
 * {@code /orders/{id}} and {@code /orders/{orderId}} match the same requests, and clients see two resources.
 */
public final class PathTemplatesAreDistinct extends OpenApiRule<ApiPath> {

    private static final Pattern PATH_VARIABLE = Pattern.compile("\\{[^}]*}");
    private static final String ANY_VARIABLE = "{}";

    @Override
    public String id() {
        return "BQ-DES-006";
    }

    @Override
    public Severity severity() {
        return Severity.MAJOR;
    }

    @Override
    public Category category() {
        return Category.DESIGN;
    }

    @Override
    protected List<ApiPath> targets(OpenApiContext document) {
        return document.paths();
    }

    @Override
    protected JsonPointer locate(ApiPath path) {
        return path.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiPath path, OpenApiContext document) {
        List<String> twins = document.paths().stream()
                .map(ApiPath::path)
                .filter(other -> !other.equals(path.path()) && template(other).equals(template(path.path())))
                .toList();
        return twins.isEmpty() ? Optional.empty() : Optional.of(sameTemplateAs(twins));
    }

    private static String template(String path) {
        return PATH_VARIABLE.matcher(path).replaceAll(ANY_VARIABLE);
    }

    private static Explanation sameTemplateAs(List<String> twins) {
        return new Explanation(
                "Path matches the same requests as " + String.join(", ", twins) + "; only the variable names differ.",
                "Give the path variable at each position one name in every mapping, for example {id}.");
    }
}
