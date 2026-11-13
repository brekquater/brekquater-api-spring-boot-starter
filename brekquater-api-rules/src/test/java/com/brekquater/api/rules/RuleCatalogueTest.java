// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Guards the promise that adding a rule is one class plus one doc page, and that neither can be forgotten. */
class RuleCatalogueTest {

    private static final String CATALOGUE_PACKAGE = "com.brekquater.api.rules.catalogue";
    private static final String RULE_PAGES_URL = "https://starter.brekquater.com/api/rules/";

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
    void everyRuleHasADocumentationPageAboutIt() {
        assertThat(rules).allSatisfy(rule -> assertThat(RULE_PAGES.resolve(rule.id() + ".md"))
                .exists()
                .content()
                .startsWith("# " + rule.id()));
    }

    @Test
    void everyFindingLinksToTheDocumentationPageOfItsRule() {
        assertThat(rules).allSatisfy(rule -> assertThat(Finding.docUrlOf(rule.id()))
                .isEqualTo(RULE_PAGES_URL + rule.id()));
    }
}
