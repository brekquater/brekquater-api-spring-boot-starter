// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.report;

import com.brekquater.api.rules.ReportStatus;
import com.brekquater.api.rules.RuleInput;
import java.util.Map;

/** What to do when a build report is missing or unreadable, in one sentence a developer can act on. */
final class ReportHints {

    private static final String GUIDE = " See https://starter.brekquater.com/api/reports";
    private static final String UNREADABLE =
            "The report was found but could not be read; the application log says why." + GUIDE;
    private static final Map<RuleInput, String> MISSING = Map.of(
            RuleInput.COVERAGE, "Run the build with the jacoco-maven-plugin report goal, for example ./mvnw verify." + GUIDE,
            RuleInput.TEST_RESULTS, "Run the tests, for example ./mvnw verify, to write Surefire reports." + GUIDE,
            RuleInput.VULNERABILITIES, "Run OSV-Scanner or OWASP Dependency-Check with JSON output." + GUIDE,
            RuleInput.BASELINE, "Commit the OpenAPI document of your last release as openapi-baseline.json." + GUIDE);

    private ReportHints() {}

    static String of(RuleInput input, ReportStatus status) {
        return status == ReportStatus.UNREADABLE ? UNREADABLE : MISSING.get(input);
    }
}
