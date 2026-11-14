// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.CoverageTargets;
import com.brekquater.api.rules.Severity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration of the Brekquater API starter. Each value is checked when it is bound, so a mistake stops the
 * application at startup with a message that names the property.
 *
 * @param enabled Whether the starter is active at all.
 * @param panel Where and to whom the assessment is shown.
 * @param rules Which rules run and how serious they are.
 * @param score How findings turn into the maturity score.
 * @param reports Where the build reports are. Each one that is not set is looked for where Maven writes it.
 * @param thresholds The coverage the coverage rules expect.
 * @param strict Whether blocker findings stop the application at startup. Off by default: the starter warns, it
 *     does not block.
 */
@ConfigurationProperties("brekquater.api")
record BrekquaterApiProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue Panel panel,
        @DefaultValue Rules rules,
        @DefaultValue Score score,
        @DefaultValue Reports reports,
        @DefaultValue Thresholds thresholds,
        @DefaultValue("false") boolean strict) {

    /**
     * Where and to whom the assessment is shown.
     *
     * @param mode Who may see the endpoint and the panel: disabled, authenticated or public. When unset, it is
     *     public while the "dev" or "local" profile is active and disabled otherwise.
     * @param basePath Path under which the assessment endpoint and the standalone page are served.
     */
    record Panel(PanelMode mode, @DefaultValue("/brekquater/api") String basePath) {

        private static final String PROPERTY = "brekquater.api.panel.base-path";

        Panel {
            if (!basePath.matches("(/[A-Za-z0-9._~-]+)+")) {
                throw new InvalidBrekquaterConfigurationException(
                        PROPERTY,
                        basePath,
                        "Use a path that starts with '/', does not end with '/', and contains only letters, digits "
                                + "and . _ ~ - between the slashes, for example /brekquater/api.");
            }
        }
    }

    /**
     * Which rules run and how serious they are.
     *
     * @param disabled Comma-separated list of rule IDs that do not run, for example BQ-DES-001.
     * @param severityOverrides Severity to use instead of a rule's own, keyed by rule ID. Only security rules can be
     *     raised to BLOCKER.
     */
    record Rules(Set<String> disabled, Map<String, Severity> severityOverrides) {

        Rules {
            disabled = disabled == null ? Set.of() : Set.copyOf(disabled);
            severityOverrides = severityOverrides == null ? Map.of() : Map.copyOf(severityOverrides);
        }
    }

    /**
     * How findings turn into the maturity score.
     *
     * @param weights How much a rule of each severity counts.
     * @param grades Lowest score for each letter grade.
     */
    record Score(@DefaultValue Weights weights, @DefaultValue Grades grades) {}

    /**
     * How much a rule of each severity counts in the score. The defaults are the OpenSSF Scorecard risk weights.
     * A weight of 0 leaves rules of that severity out of the score.
     *
     * @param blocker Weight of BLOCKER rules.
     * @param major Weight of MAJOR rules.
     * @param minor Weight of MINOR rules.
     * @param info Weight of INFO rules.
     */
    record Weights(
            @DefaultValue("10") BigDecimal blocker,
            @DefaultValue("7.5") BigDecimal major,
            @DefaultValue("5") BigDecimal minor,
            @DefaultValue("0") BigDecimal info) {

        private static final String PROPERTY_PREFIX = "brekquater.api.score.weights.";

        Weights {
            requireNotNegative("blocker", blocker);
            requireNotNegative("major", major);
            requireNotNegative("minor", minor);
            requireNotNegative("info", info);
        }

        private static void requireNotNegative(String name, BigDecimal weight) {
            if (weight.signum() < 0) {
                throw new InvalidBrekquaterConfigurationException(
                        PROPERTY_PREFIX + name, weight, "Use a weight of 0 or more.");
            }
        }
    }

    /**
     * Lowest score, from 0 to 100, that earns each letter grade. A score below the D minimum earns F.
     *
     * @param a Lowest score for grade A.
     * @param b Lowest score for grade B.
     * @param c Lowest score for grade C.
     * @param d Lowest score for grade D.
     */
    record Grades(
            @DefaultValue("90") int a, @DefaultValue("75") int b, @DefaultValue("60") int c, @DefaultValue("40") int d) {

        private static final String PROPERTY = "brekquater.api.score.grades";
        private static final int MAXIMUM_SCORE = 100;

        Grades {
            boolean descending = MAXIMUM_SCORE >= a && a >= b && b >= c && c >= d && d >= 0;
            if (!descending) {
                throw new InvalidBrekquaterConfigurationException(
                        PROPERTY,
                        "a=" + a + ", b=" + b + ", c=" + c + ", d=" + d,
                        "Use minimums between 0 and 100 with a >= b >= c >= d, for example the defaults 90, 75, 60, 40.");
            }
        }
    }

    /**
     * Where the build reports are. Paths are relative to the working directory unless absolute. A report that is
     * not found is never an error; the rules that need it are skipped.
     *
     * @param coverage JaCoCo XML report. Defaults to target/site/jacoco/jacoco.xml.
     * @param testResults Surefire or Failsafe XML reports: files, or directories whose TEST-*.xml files are read.
     *     Defaults to target/surefire-reports and target/failsafe-reports.
     * @param vulnerabilities OSV-Scanner or OWASP Dependency-Check JSON report; the format is recognised from the
     *     content. Defaults to target/osv-scanner.json, then target/dependency-check-report.json.
     */
    record Reports(String coverage, List<String> testResults, String vulnerabilities) {

        Reports {
            coverage = coverage == null ? "" : coverage;
            testResults = testResults == null ? List.of() : List.copyOf(testResults);
            vulnerabilities = vulnerabilities == null ? "" : vulnerabilities;
        }
    }

    /**
     * The coverage, in whole percent, that the coverage rules expect.
     *
     * @param lineCoverage Lowest acceptable line coverage, checked by BQ-TST-001.
     * @param branchCoverage Lowest acceptable branch coverage, checked by BQ-TST-002.
     */
    record Thresholds(@DefaultValue("80") int lineCoverage, @DefaultValue("70") int branchCoverage) {

        private static final String PROPERTY_PREFIX = "brekquater.api.thresholds.";
        private static final int MAXIMUM = 100;

        Thresholds {
            requirePercent("line-coverage", lineCoverage);
            requirePercent("branch-coverage", branchCoverage);
        }

        CoverageTargets coverageTargets() {
            return new CoverageTargets(lineCoverage, branchCoverage);
        }

        private static void requirePercent(String name, int value) {
            if (value < 0 || value > MAXIMUM) {
                throw new InvalidBrekquaterConfigurationException(
                        PROPERTY_PREFIX + name, value, "Use a whole percentage from 0 to 100.");
            }
        }
    }
}
