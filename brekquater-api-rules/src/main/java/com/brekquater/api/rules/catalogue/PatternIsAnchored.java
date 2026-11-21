// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiSchemaNode;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.JsonPointer;
import com.brekquater.api.rules.OpenApiContext;
import com.brekquater.api.rules.OpenApiRule;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;

/**
 * BQ-DES-009: a schema {@code pattern} is anchored with {@code ^} and {@code $}. Bean Validation's {@code @Pattern}
 * must match the whole value, but JSON Schema only searches for a match, so the same unanchored regular expression
 * accepts more in a client than on the server.
 */
public final class PatternIsAnchored extends OpenApiRule<ApiSchemaNode> {

    private static final String START = "^";
    private static final String END = "$";
    private static final char BACKSLASH = '\\';
    private static final Explanation UNANCHORED = new Explanation(
            "Pattern is not anchored, so JSON Schema accepts any value that merely contains a match.",
            "Start the regular expression with ^ and end it with $, for example @Pattern(regexp = \"^[A-Z]+$\"). "
                    + "Bean Validation already matches the whole value, so the server behaves the same.");

    @Override
    public String id() {
        return "BQ-DES-009";
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
        return document.schemas().stream().filter(schema -> schema.pattern().isPresent()).toList();
    }

    @Override
    protected JsonPointer locate(ApiSchemaNode schema) {
        return schema.pointer();
    }

    @Override
    protected Optional<Explanation> check(ApiSchemaNode schema, OpenApiContext document) {
        return schema.pattern().filter(PatternIsAnchored::anchored).isPresent() ? Optional.empty() : Optional.of(UNANCHORED);
    }

    private static boolean anchored(String pattern) {
        return pattern.startsWith(START) && pattern.endsWith(END) && !escaped(pattern, pattern.length() - 1);
    }

    // A character is escaped when an odd number of backslashes precede it: "\$" is a dollar sign, "\\$" an anchor.
    private static boolean escaped(String pattern, int position) {
        int backslashes = 0;
        while (position - backslashes > 0 && pattern.charAt(position - backslashes - 1) == BACKSLASH) {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }
}
