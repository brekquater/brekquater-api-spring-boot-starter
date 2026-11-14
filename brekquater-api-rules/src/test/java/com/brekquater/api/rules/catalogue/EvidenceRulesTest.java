// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.Advisory;
import com.brekquater.api.rules.AssessmentContext;
import com.brekquater.api.rules.BuildReport;
import com.brekquater.api.rules.BuildReports;
import com.brekquater.api.rules.ClassCoverage;
import com.brekquater.api.rules.Coverage;
import com.brekquater.api.rules.CoverageCounter;
import com.brekquater.api.rules.CoverageTargets;
import com.brekquater.api.rules.CvssBand;
import com.brekquater.api.rules.FailedTest;
import com.brekquater.api.rules.Finding;
import com.brekquater.api.rules.ReportFile;
import com.brekquater.api.rules.RuleInput;
import com.brekquater.api.rules.RuleResult;
import com.brekquater.api.rules.TestResults;
import com.brekquater.api.rules.Vulnerabilities;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EvidenceRulesTest {

    private static final ReportFile JACOCO = new ReportFile("jacoco.xml", Instant.EPOCH);
    private static final ReportFile SUREFIRE = new ReportFile("surefire-reports", Instant.EPOCH);
    private static final ReportFile OSV = new ReportFile("osv-scanner.json", Instant.EPOCH);

    private static final List<ClassCoverage> CLASSES = List.of(
            new ClassCoverage("com.example.Small", new CoverageCounter(1, 9)),
            new ClassCoverage("com.example.Worst", new CoverageCounter(40, 10)),
            new ClassCoverage("com.example.Second", new CoverageCounter(20, 30)),
            new ClassCoverage("com.example.Third", new CoverageCounter(5, 45)),
            new ClassCoverage("com.example.Covered", new CoverageCounter(0, 50)));

    @Test
    void lineCoveragePassesAtExactlyTheTarget() {
        RuleResult result = new LineCoverageMeetsTarget().evaluate(withCoverage(new CoverageCounter(20, 80)));

        assertThat(result.applicableTargets()).isEqualTo(1);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void lineCoverageJustBelowTheTargetFailsEvenWhenItRoundsUp() {
        RuleResult result = new LineCoverageMeetsTarget().evaluate(withCoverage(new CoverageCounter(2001, 7999)));

        assertThat(result.findings()).hasSize(1);
    }

    @Test
    void lineCoverageNamesTheThreeClassesThatMissTheMostLines() {
        RuleResult result = new LineCoverageMeetsTarget().evaluate(withCoverage(new CoverageCounter(66, 144)));

        assertThat(result.findings())
                .singleElement()
                .satisfies(finding -> {
                    assertThat(finding.ruleId()).isEqualTo("BQ-TST-001");
                    assertThat(finding.evidence().source()).isEqualTo(RuleInput.COVERAGE);
                    assertThat(finding.evidence().location()).isEqualTo("jacoco.xml");
                    assertThat(finding.message())
                            .isEqualTo("Line coverage is 68%, below the target of 80%. Most missed lines: "
                                    + "com.example.Worst (40), com.example.Second (20), com.example.Third (5).");
                });
    }

    @Test
    void lineCoverageFollowsAConfiguredTarget() {
        AssessmentContext context =
                withCoverage(new CoverageCounter(40, 60)).withCoverageTargets(new CoverageTargets(60, 0));

        assertThat(new LineCoverageMeetsTarget().evaluate(context).findings()).isEmpty();
    }

    @Test
    void branchCoverageReportsAShortfallWithoutNamingClasses() {
        RuleResult result = new BranchCoverageMeetsTarget().evaluate(withCoverage(new CoverageCounter(1, 1)));

        assertThat(result.findings())
                .singleElement()
                .satisfies(finding -> {
                    assertThat(finding.ruleId()).isEqualTo("BQ-TST-002");
                    assertThat(finding.message()).isEqualTo("Branch coverage is 50%, below the target of 70%.");
                });
    }

    @Test
    void branchCoverageDoesNotApplyToAProjectWithoutBranches() {
        Coverage coverage = new Coverage(new CoverageCounter(0, 1), new CoverageCounter(0, 0), List.of());
        AssessmentContext context = AssessmentContext.empty()
                .withReports(new BuildReports(
                        BuildReport.read(coverage, JACOCO), BuildReport.missing(), BuildReport.missing()));

        assertThat(new BranchCoverageMeetsTarget().evaluate(context).applicableTargets()).isZero();
    }

    @Test
    void testsPassCountsEveryTestThatRanAsATarget() {
        TestResults results = new TestResults(10, 2, List.of(new FailedTest("com.example.OrdersTest", "listsOrders")));

        RuleResult result = new TestsPass().evaluate(withTests(results));

        assertThat(result.applicableTargets()).isEqualTo(8);
        assertThat(result.passingTargets()).isEqualTo(7);
    }

    @Test
    void testsPassReportsEachFailedTestAtItsClassAndMethod() {
        TestResults results = new TestResults(3, 0, List.of(
                new FailedTest("com.example.OrdersTest", "listsOrders"),
                new FailedTest("com.example.OrdersIT", "savesAnOrder")));

        RuleResult result = new TestsPass().evaluate(withTests(results));

        assertThat(result.findings())
                .extracting(finding -> finding.evidence().location())
                .containsExactly("com.example.OrdersTest#listsOrders", "com.example.OrdersIT#savesAnOrder");
    }

    @Test
    void eachVulnerabilityRuleReportsOnlyItsOwnBand() {
        AssessmentContext context = withAdvisories(
                advisory("CVE-2099-0001", CvssBand.CRITICAL), advisory("CVE-2099-0002", CvssBand.HIGH));

        assertThat(new NoCriticalVulnerability().evaluate(context).findings())
                .extracting(Finding::message)
                .containsExactly("pkg:maven/com.example/parser@1.0.0 has a Critical known vulnerability, "
                        + "CVE-2099-0001 (CVSS 9.8).");
        assertThat(new NoHighVulnerability().evaluate(context).findings()).hasSize(1);
        assertThat(new NoMediumVulnerability().evaluate(context).findings()).isEmpty();
        assertThat(new NoLowVulnerability().evaluate(context).findings()).isEmpty();
    }

    @Test
    void aVulnerabilityRuleChecksTheDependencySetAsOneTarget() {
        AssessmentContext context = withAdvisories(
                advisory("CVE-2099-0001", CvssBand.HIGH), advisory("CVE-2099-0002", CvssBand.HIGH));

        RuleResult result = new NoHighVulnerability().evaluate(context);

        assertThat(result.applicableTargets()).isEqualTo(1);
        assertThat(result.passingTargets()).isZero();
        assertThat(result.findings()).hasSize(2);
    }

    @Test
    void aVulnerabilityFindingPointsAtTheDependencyAndNamesTheFix() {
        AssessmentContext context = withAdvisories(advisory("CVE-2099-0001", CvssBand.CRITICAL));

        assertThat(new NoCriticalVulnerability().evaluate(context).findings())
                .singleElement()
                .satisfies(finding -> {
                    assertThat(finding.evidence().source()).isEqualTo(RuleInput.VULNERABILITIES);
                    assertThat(finding.evidence().location()).isEqualTo("pkg:maven/com.example/parser@1.0.0");
                    assertThat(finding.remediation()).startsWith("Upgrade to 2.0 or later.");
                });
    }

    @Test
    void aVulnerabilityWithoutAKnownFixSaysSoAndShowsThatItHasNoScore() {
        Advisory unscored = new Advisory(
                "GHSA-aaaa-bbbb-cccc", "pkg:maven/com.example/parser@1.0.0", CvssBand.MEDIUM, Optional.empty(),
                Optional.empty());

        assertThat(new NoMediumVulnerability().evaluate(withAdvisories(unscored)).findings())
                .singleElement()
                .satisfies(finding -> {
                    assertThat(finding.message()).endsWith("GHSA-aaaa-bbbb-cccc (no CVSS score).");
                    assertThat(finding.remediation()).startsWith("No fixed version is known yet.");
                });
    }

    @Test
    void anEmptyScanPassesEveryVulnerabilityRule() {
        RuleResult result = new NoCriticalVulnerability().evaluate(withAdvisories());

        assertThat(result.applicableTargets()).isEqualTo(1);
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void noEvidenceRuleAppliesWithoutItsReport() {
        AssessmentContext nothing = AssessmentContext.empty();

        assertThat(List.of(new LineCoverageMeetsTarget(), new BranchCoverageMeetsTarget(), new TestsPass(),
                        new NoCriticalVulnerability()))
                .allSatisfy(rule -> assertThat(rule.evaluate(nothing)).isEqualTo(RuleResult.notApplicable()));
    }

    private static AssessmentContext withCoverage(CoverageCounter counter) {
        Coverage coverage = new Coverage(counter, counter, CLASSES);
        return AssessmentContext.empty()
                .withReports(new BuildReports(
                        BuildReport.read(coverage, JACOCO), BuildReport.missing(), BuildReport.missing()));
    }

    private static AssessmentContext withTests(TestResults results) {
        return AssessmentContext.empty()
                .withReports(new BuildReports(
                        BuildReport.missing(), BuildReport.read(results, SUREFIRE), BuildReport.missing()));
    }

    private static AssessmentContext withAdvisories(Advisory... advisories) {
        Vulnerabilities vulnerabilities = new Vulnerabilities(List.of(advisories));
        return AssessmentContext.empty()
                .withReports(new BuildReports(
                        BuildReport.missing(), BuildReport.missing(), BuildReport.read(vulnerabilities, OSV)));
    }

    private static Advisory advisory(String id, CvssBand band) {
        BigDecimal score = band == CvssBand.CRITICAL ? new BigDecimal("9.8") : new BigDecimal("7.5");
        return new Advisory(id, "pkg:maven/com.example/parser@1.0.0", band, Optional.of(score), Optional.of("2.0"));
    }
}
