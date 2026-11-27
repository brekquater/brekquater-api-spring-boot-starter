// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.BuildReport;
import com.brekquater.api.rules.BuildReports;
import com.brekquater.api.rules.ReportFile;
import com.brekquater.api.rules.ReportStatus;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildReportFilesTest {

    private static final String JACOCO = """
            <report name="orders"><counter type="LINE" missed="1" covered="9"/></report>""";
    private static final String PASSING_SUITE = """
            <testsuite name="A"><testcase name="one" classname="A"/></testsuite>""";
    private static final String FAILING_SUITE = """
            <testsuite name="B"><testcase name="two" classname="B"><failure/></testcase></testsuite>""";
    private static final String OSV_SCANNER = """
            {"results": []}""";

    @TempDir
    Path project;

    @Test
    void findsNothingWhenNoReportExists() {
        BuildReports reports = filesAt(defaults()).read();

        assertThat(reports.coverage().status()).isEqualTo(ReportStatus.MISSING);
        assertThat(reports.testResults().status()).isEqualTo(ReportStatus.MISSING);
        assertThat(reports.vulnerabilities().status()).isEqualTo(ReportStatus.MISSING);
    }

    @Test
    void readsTheCoverageReportAndNamesItByFileNameOnly() throws IOException {
        Instant written = Instant.parse("2026-11-13T09:58:00Z");
        Path report = write("target/site/jacoco/jacoco.xml", JACOCO);
        Files.setLastModifiedTime(report, FileTime.from(written));

        BuildReports reports = filesAt(defaults()).read();

        assertThat(reports.coverage().status()).isEqualTo(ReportStatus.READ);
        assertThat(reports.coverage().file()).contains(new ReportFile("jacoco.xml", written));
    }

    @Test
    void combinesEveryTestReportInTheSurefireAndFailsafeDirectories() throws IOException {
        write("target/surefire-reports/TEST-A.xml", PASSING_SUITE);
        write("target/surefire-reports/A.txt", "not a report");
        write("target/failsafe-reports/TEST-B.xml", FAILING_SUITE);

        BuildReports reports = filesAt(defaults()).read();

        assertThat(reports.testResults().content()).hasValueSatisfying(results -> {
            assertThat(results.tests()).isEqualTo(2);
            assertThat(results.failed()).hasSize(1);
        });
        assertThat(reports.testResults().file().map(ReportFile::name)).contains("surefire-reports, failsafe-reports");
    }

    @Test
    void readsTheFirstVulnerabilityReportThatExists() throws IOException {
        write("target/dependency-check-report.json", "{\"reportSchema\": \"1.1\", \"dependencies\": []}");

        BuildReports reports = filesAt(defaults()).read();

        assertThat(reports.vulnerabilities().file().map(ReportFile::name)).contains("dependency-check-report.json");
    }

    @Test
    void marksAMalformedReportAsUnreadableAndKeepsItsName() throws IOException {
        write("target/osv-scanner.json", "{\"results\": [");

        BuildReports reports = filesAt(defaults()).read();

        assertThat(reports.vulnerabilities().status()).isEqualTo(ReportStatus.UNREADABLE);
        assertThat(reports.vulnerabilities().file().map(ReportFile::name)).contains("osv-scanner.json");
    }

    @Test
    void marksAllTestResultsAsUnreadableWhenOneTestReportIsBroken() throws IOException {
        write("target/surefire-reports/TEST-A.xml", PASSING_SUITE);
        write("target/surefire-reports/TEST-B.xml", "<testsuite>");

        BuildReports reports = filesAt(defaults()).read();

        assertThat(reports.testResults().status()).isEqualTo(ReportStatus.UNREADABLE);
    }

    @Test
    void refusesAReportLargerThan50Megabytes() throws IOException {
        Path report = write("target/osv-scanner.json", OSV_SCANNER);
        try (RandomAccessFile file = new RandomAccessFile(report.toFile(), "rw")) {
            file.setLength(BuildReportFiles.MAXIMUM_BYTES + 1);
        }

        BuildReports reports = filesAt(defaults()).read();

        assertThat(reports.vulnerabilities().status()).isEqualTo(ReportStatus.UNREADABLE);
    }

    @Test
    void readsReportsFromConfiguredLocations() throws IOException {
        write("build/coverage.xml", JACOCO);
        write("build/results.xml", PASSING_SUITE);
        write("build/scan.json", OSV_SCANNER);
        ReportLocations configured = ReportLocations.of(new BrekquaterApiProperties.Reports(
                project.resolve("build/coverage.xml").toString(),
                List.of(project.resolve("build/results.xml").toString()),
                project.resolve("build/scan.json").toString(),
                project.resolve("build/baseline.yaml").toString()));

        BuildReports reports = new BuildReportFiles(configured).read();

        assertThat(reports.coverage().status()).isEqualTo(ReportStatus.READ);
        assertThat(reports.testResults().file().map(ReportFile::name)).contains("results.xml");
        assertThat(reports.vulnerabilities().status()).isEqualTo(ReportStatus.READ);
    }

    @Test
    void theFingerprintChangesWhenAReportAppearsOrIsRewritten() throws IOException {
        BuildReportFiles files = filesAt(defaults());
        String before = files.fingerprint();
        Path report = write("target/site/jacoco/jacoco.xml", JACOCO);
        String appeared = files.fingerprint();
        Files.setLastModifiedTime(report, FileTime.from(Instant.parse("2030-01-01T00:00:00Z")));

        assertThat(appeared).isNotEqualTo(before);
        assertThat(files.fingerprint()).isNotEqualTo(appeared);
    }

    @Test
    void readsTheBaselineDocumentAsJsonFromItsDefaultLocation() throws IOException {
        write("openapi-baseline.json", """
                {"openapi": "3.0.1", "info": {"title": "Orders API", "version": "1.0.0"}, "paths": {}}
                """);

        BuildReport<OpenAPI> baseline = filesAt(defaults()).baseline();

        assertThat(baseline.content().map(openApi -> openApi.getInfo().getVersion())).contains("1.0.0");
        assertThat(baseline.file().map(ReportFile::name)).contains("openapi-baseline.json");
    }

    @Test
    void readsAConfiguredBaselineWrittenInYaml() throws IOException {
        write("build/baseline.yaml", """
                openapi: 3.1.0
                info: {title: Orders API, version: 2.0.0}
                paths:
                  /orders:
                    get: {responses: {'200': {description: OK}}}
                """);
        ReportLocations configured = ReportLocations.of(new BrekquaterApiProperties.Reports(
                null, null, null, project.resolve("build/baseline.yaml").toString()));

        BuildReport<OpenAPI> baseline = new BuildReportFiles(configured).baseline();

        assertThat(baseline.content().map(openApi -> openApi.getPaths().keySet())).contains(Set.of("/orders"));
    }

    @Test
    void marksABaselineThatIsNotAnOpenApiDocumentUnreadable() throws IOException {
        write("openapi-baseline.json", "{\"orders\": []}");

        assertThat(filesAt(defaults()).baseline().status()).isEqualTo(ReportStatus.UNREADABLE);
    }

    @Test
    void readsNothingWhenToldToLookNowhere() throws IOException {
        write("target/site/jacoco/jacoco.xml", JACOCO);

        assertThat(new BuildReportFiles(ReportLocations.NONE).read()).isEqualTo(BuildReports.none());
    }

    private ReportLocations defaults() {
        ReportLocations maven = ReportLocations.of(new BrekquaterApiProperties.Reports(null, null, null, null));
        return new ReportLocations(
                maven.coverage().map(project::resolve),
                maven.testResults().stream().map(project::resolve).toList(),
                maven.vulnerabilities().stream().map(project::resolve).toList(),
                maven.baseline().map(project::resolve));
    }

    private static BuildReportFiles filesAt(ReportLocations locations) {
        return new BuildReportFiles(locations);
    }

    private Path write(String relativePath, String content) throws IOException {
        Path file = project.resolve(relativePath);
        Files.createDirectories(file.getParent());
        return Files.writeString(file, content);
    }
}
