// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guards the promise that adding a rule is one class plus one doc page, and that neither can be forgotten. The page's
 * front matter is what the documentation site builds the catalogue and the sidebar from, so it must agree with the
 * rule's code.
 */
class RuleCatalogueTest {

    private static final String CATALOGUE_PACKAGE = "com.brekquater.api.rules.catalogue";
    private static final String RULE_PAGES_URL = "https://starter.brekquater.com/api/rules/";
    private static final String FRONT_MATTER_FENCE = "---";

    // Surefire runs from the module directory; the rule pages live in the repository's docs directory.
    private static final Path RULE_PAGES = Path.of("..", "docs", "rules");

    private final List<Rule> rules = RuleRegistry.fromServiceLoader().rules();

    @Test
    void everyRuleClassInTheCatalogueIsRegisteredForDiscovery() {
        List<String> ruleClasses = new ClassFileImporter()
                .importPackages(CATALOGUE_PACKAGE).stream()
                        .filter(candidate -> candidate.isAssignableTo(Rule.class))
                        .filter(candidate -> !candidate.getModifiers().contains(JavaModifier.ABSTRACT))
                        .map(JavaClass::getName)
                        .toList();

        assertThat(rules).extracting(rule -> rule.getClass().getName()).containsAll(ruleClasses);
    }

    @Test
    void everyRuleIdFollowsTheNamingScheme() {
        assertThat(rules).extracting(Rule::id).allMatch(id -> id.matches("BQ-[A-Z]{3}-\\d{3}"));
    }

    @Test
    void everyRuleHasADocumentationPageAtItsPermanentAddress() {
        assertThat(rules).allSatisfy(rule -> assertThat(frontMatterOf(rule))
                .contains("rule_id: " + rule.id(), "slug: /rules/" + rule.id())
                .anySatisfy(line -> assertThat(line).startsWith("title: \"" + rule.id() + ": ")));
    }

    @Test
    void everyRulePageStatesTheSeverityCategoryAndInputOfItsRule() {
        assertThat(rules).allSatisfy(rule -> assertThat(frontMatterOf(rule)).contains(
                "severity: " + rule.severity(), "category: " + rule.category(), "reads: " + rule.input()));
    }

    @Test
    void everyFindingLinksToTheDocumentationPageOfItsRule() {
        assertThat(rules).allSatisfy(rule -> assertThat(Finding.docUrlOf(rule.id()))
                .isEqualTo(RULE_PAGES_URL + rule.id()));
    }

    // The front matter is the block between the page's first line, "---", and the next "---" line.
    private static List<String> frontMatterOf(Rule rule) {
        List<String> lines = readLines(RULE_PAGES.resolve(rule.id() + ".md"));
        assertThat(lines).as("front matter of %s", rule.id()).first().isEqualTo(FRONT_MATTER_FENCE);
        int closingFence = lines.subList(1, lines.size()).indexOf(FRONT_MATTER_FENCE) + 1;
        assertThat(closingFence).as("closing front matter fence of %s", rule.id()).isPositive();
        return lines.subList(1, closingFence);
    }

    private static List<String> readLines(Path page) {
        try {
            return Files.readAllLines(page);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
