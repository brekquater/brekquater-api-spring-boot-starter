// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BuildReportModelTest {

    @Test
    void bandsCvssScoresAtTheBoundariesOfTheQualitativeScale() {
        assertThat(CvssBand.of(new BigDecimal("10.0"))).isEqualTo(CvssBand.CRITICAL);
        assertThat(CvssBand.of(new BigDecimal("9.0"))).isEqualTo(CvssBand.CRITICAL);
        assertThat(CvssBand.of(new BigDecimal("8.9"))).isEqualTo(CvssBand.HIGH);
        assertThat(CvssBand.of(new BigDecimal("7.0"))).isEqualTo(CvssBand.HIGH);
        assertThat(CvssBand.of(new BigDecimal("6.9"))).isEqualTo(CvssBand.MEDIUM);
        assertThat(CvssBand.of(new BigDecimal("4.0"))).isEqualTo(CvssBand.MEDIUM);
        assertThat(CvssBand.of(new BigDecimal("3.9"))).isEqualTo(CvssBand.LOW);
        assertThat(CvssBand.of(new BigDecimal("0.1"))).isEqualTo(CvssBand.LOW);
        assertThat(CvssBand.of(BigDecimal.ZERO)).isEqualTo(CvssBand.NONE);
    }

    @Test
    void readsBandNamesInAnyCaseAndGitHubsModerate() {
        assertThat(CvssBand.named("high")).contains(CvssBand.HIGH);
        assertThat(CvssBand.named(" Moderate ")).contains(CvssBand.MEDIUM);
        assertThat(CvssBand.named("Unknown")).isEmpty();
        assertThat(CvssBand.named("")).isEmpty();
    }

    @Test
    void comparesCoverageWithTheTargetExactly() {
        assertThat(new CoverageCounter(1, 4).meets(80)).isTrue();
        assertThat(new CoverageCounter(2001, 7999).meets(80)).isFalse();
        assertThat(new CoverageCounter(2001, 7999).percent()).isEqualTo(79);
    }

    @Test
    void treatsAnEmptyCounterAsFullyCovered() {
        assertThat(new CoverageCounter(0, 0).percent()).isEqualTo(100);
        assertThat(new CoverageCounter(0, 0).meets(100)).isTrue();
    }

    @Test
    void ranksOnlyClassesWithMissedLines() {
        Coverage coverage = new Coverage(new CoverageCounter(3, 3), new CoverageCounter(0, 0), List.of(
                new ClassCoverage("b.Second", new CoverageCounter(1, 1)),
                new ClassCoverage("a.Covered", new CoverageCounter(0, 1)),
                new ClassCoverage("a.First", new CoverageCounter(1, 1)),
                new ClassCoverage("c.Worst", new CoverageCounter(5, 1))));

        assertThat(coverage.classesWithMostMissedLines(5))
                .extracting(ClassCoverage::name)
                .containsExactly("c.Worst", "a.First", "b.Second");
    }

    @Test
    void addsTestResultsTogether() {
        TestResults unit = new TestResults(5, 1, List.of(new FailedTest("A", "one")));
        TestResults integration = new TestResults(2, 1, List.of(new FailedTest("B", "two")));

        TestResults all = unit.plus(integration);

        assertThat(all.tests()).isEqualTo(7);
        assertThat(all.skipped()).isEqualTo(2);
        assertThat(all.passed()).isEqualTo(3);
        assertThat(all.failed()).hasSize(2);
    }

    @Test
    void rejectsTestCountsThatDoNotAddUp() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TestResults(1, 1, List.of(new FailedTest("A", "b"))));
        assertThatIllegalArgumentException().isThrownBy(() -> new TestResults(1, -1, List.of()));
        assertThat(new TestResults(2, 1, List.of(new FailedTest("A", "b"))).passed()).isZero();
    }

    @Test
    void acceptsCoverageTargetsFrom0To100Only() {
        assertThat(new CoverageTargets(0, 100).branchCoverage()).isEqualTo(100);
        assertThatIllegalArgumentException().isThrownBy(() -> new CoverageTargets(101, 70));
        assertThatIllegalArgumentException().isThrownBy(() -> new CoverageTargets(80, -1));
    }

    @Test
    void keepsAdvisoriesInAFixedOrderWithoutRepeats() {
        Advisory first = advisory("pkg:maven/a/a@1", "CVE-2099-0002");
        Advisory second = advisory("pkg:maven/a/a@1", "CVE-2099-0003");
        Advisory third = advisory("pkg:maven/b/b@1", "CVE-2099-0001");

        Vulnerabilities vulnerabilities = new Vulnerabilities(List.of(third, second, first, third));

        assertThat(vulnerabilities.advisories()).containsExactly(first, second, third);
    }

    @Test
    void aReportHasContentExactlyWhenItWasRead() {
        ReportFile file = new ReportFile("jacoco.xml", Instant.EPOCH);

        assertThat(BuildReport.unreadable(file).content()).isEmpty();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BuildReport<>(ReportStatus.MISSING, Optional.of("x"), Optional.empty()));
    }

    @Test
    void aContextProvidesExactlyTheReportsThatWereRead() {
        AssessmentContext context = AssessmentContext.empty()
                .withReports(new BuildReports(
                        BuildReport.missing(),
                        BuildReport.read(TestResults.none(), new ReportFile("surefire-reports", Instant.EPOCH)),
                        BuildReport.missing()));

        assertThat(context.provides(RuleInput.TEST_RESULTS)).isTrue();
        assertThat(context.provides(RuleInput.COVERAGE)).isFalse();
        assertThat(context.provides(RuleInput.VULNERABILITIES)).isFalse();
    }

    private static Advisory advisory(String packageUrl, String id) {
        return new Advisory(id, packageUrl, CvssBand.HIGH, Optional.empty(), Optional.empty());
    }
}
