// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

/**
 * Artefact a rule reads. The engine skips a rule whose input is absent instead of calling it, so the report can say
 * why a category was not assessed.
 */
public enum RuleInput {
    /** The application's OpenAPI document. */
    OPENAPI,
    /** A JaCoCo XML coverage report from the application's build. */
    COVERAGE,
    /** Surefire or Failsafe XML test reports from the application's build. */
    TEST_RESULTS,
    /** An OSV-Scanner or OWASP Dependency-Check report on the application's dependencies. */
    VULNERABILITIES,
    /** The OpenAPI document of the last release, committed with the application and compared with the current one. */
    BASELINE
}
