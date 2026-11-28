// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiInfo;
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
 * BQ-DOC-011: {@code info.version} is a Semantic Versioning 2.0 version such as {@code 1.4.0}, or a release date
 * such as {@code 2026-11-27}. Tools that compare two versions of the API need a version they can order. A missing
 * version or springdoc's {@code v0} is left to BQ-DOC-004.
 */
public final class InfoVersionIsSemantic extends OpenApiRule<ApiInfo> {

    private static final String PLACEHOLDER = "v0";
    // The regular expression semver.org recommends, without the named groups.
    private static final Pattern SEMANTIC = Pattern.compile(
            "(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"
                    + "(-((0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(\\.(0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?"
                    + "(\\+([0-9a-zA-Z-]+(\\.[0-9a-zA-Z-]+)*))?");
    private static final Pattern DATED = Pattern.compile("\\d{4}-\\d{2}-\\d{2}(-preview)?");

    @Override
    public String id() {
        return "BQ-DOC-011";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public Category category() {
        return Category.DOCUMENTATION;
    }

    @Override
    protected List<ApiInfo> targets(OpenApiContext document) {
        return document.info().version()
                .filter(version -> !version.isBlank() && !version.strip().equals(PLACEHOLDER))
                .map(version -> List.of(document.info()))
                .orElse(List.of());
    }

    @Override
    protected JsonPointer locate(ApiInfo target) {
        return target.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiInfo target, OpenApiContext document) {
        String version = target.version().orElseThrow().strip();
        if (SEMANTIC.matcher(version).matches() || DATED.matcher(version).matches()) {
            return Optional.empty();
        }
        return Optional.of(new Explanation(
                "API version " + version + " is neither MAJOR.MINOR.PATCH nor a date.",
                "Write all three numbers, for example new Info().version(\"1.4.0\"), or feed it from the build with "
                        + "BuildProperties.getVersion()."));
    }
}
