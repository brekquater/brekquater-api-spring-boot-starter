// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private static final String ADAPTER_PACKAGE = "com.brekquater.api.rules.openapi..";
    private static final String CATALOGUE_PACKAGE = "com.brekquater.api.rules.catalogue..";
    private static final String REPORT_PACKAGE = "com.brekquater.api.rules.report..";
    private static final String EVIDENCE_PACKAGE = "com.brekquater.api.rules.evidence..";

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.brekquater.api.rules");

    @Test
    void theRulesModuleDoesNotDependOnSpring() {
        noClasses()
                .should()
                .dependOnClassesThat()
                .resideInAPackage("org.springframework..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void onlyTheAdapterPackageTouchesTheOpenApiLibrary() {
        noClasses()
                .that()
                .resideOutsideOfPackage(ADAPTER_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.swagger..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void rulesSeeTheDocumentOnlyThroughTheOpenApiContext() {
        noClasses()
                .that()
                .resideInAPackage(CATALOGUE_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("io.swagger..", ADAPTER_PACKAGE)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void onlyTheReportAndEvidencePackagesTouchTheJsonLibrary() {
        noClasses()
                .that()
                .resideOutsideOfPackages(REPORT_PACKAGE, EVIDENCE_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("tools.jackson..", "com.fasterxml.jackson..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void rulesSeeBuildReportsOnlyThroughTheAssessmentContext() {
        noClasses()
                .that()
                .resideInAPackage(CATALOGUE_PACKAGE)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(EVIDENCE_PACKAGE)
                .check(PRODUCTION_CLASSES);
    }
}
