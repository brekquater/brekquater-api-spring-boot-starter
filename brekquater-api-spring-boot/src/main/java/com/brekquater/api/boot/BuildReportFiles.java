// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.BuildReport;
import com.brekquater.api.rules.BuildReports;
import com.brekquater.api.rules.Coverage;
import com.brekquater.api.rules.ReportFile;
import com.brekquater.api.rules.TestResults;
import com.brekquater.api.rules.Vulnerabilities;
import com.brekquater.api.rules.evidence.JacocoXmlReport;
import com.brekquater.api.rules.evidence.JunitXmlReport;
import com.brekquater.api.rules.evidence.UnreadableReportException;
import com.brekquater.api.rules.evidence.VulnerabilityReport;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Finds the build reports on disk and reads them. Only file names ever leave this class: the reports it returns and
 * the warnings it logs never contain a directory, so they reveal nothing about the machine's layout.
 */
final class BuildReportFiles {

    // Real reports are far smaller; the limit only stops a stray file from exhausting memory.
    static final long MAXIMUM_BYTES = 50L * 1024 * 1024;

    private static final Log LOGGER = LogFactory.getLog(BuildReportFiles.class);
    private static final String TEST_REPORT_PREFIX = "TEST-";
    private static final String TEST_REPORT_SUFFIX = ".xml";

    private final ReportLocations locations;

    BuildReportFiles(ReportLocations locations) {
        this.locations = locations;
    }

    /** Returns a value that changes whenever a report appears, disappears or is rewritten; it only reads metadata. */
    String fingerprint() {
        return Stream.of(locations.coverage().stream(), testReports().stream(), vulnerabilityReport().stream(),
                        locations.baseline().stream())
                .flatMap(Function.identity())
                .map(StampedFile::of)
                .flatMap(Optional::stream)
                .map(StampedFile::fingerprint)
                .collect(Collectors.joining("|"));
    }

    BuildReports read() {
        return new BuildReports(coverage(), testResults(), vulnerabilities());
    }

    /** Returns the OpenAPI document of the last release, which the assessment compares with the current one. */
    BuildReport<OpenAPI> baseline() {
        return locations.baseline()
                .flatMap(StampedFile::of)
                .map(file -> readOne(file, BaselineDocumentReader::read))
                .orElse(BuildReport.missing());
    }

    private BuildReport<Coverage> coverage() {
        return locations.coverage()
                .flatMap(StampedFile::of)
                .map(file -> readOne(file, JacocoXmlReport::read))
                .orElse(BuildReport.missing());
    }

    private BuildReport<Vulnerabilities> vulnerabilities() {
        return vulnerabilityReport()
                .flatMap(StampedFile::of)
                .map(file -> readOne(file, VulnerabilityReport::read))
                .orElse(BuildReport.missing());
    }

    private BuildReport<TestResults> testResults() {
        List<StampedFile> files = testReports().stream().map(StampedFile::of).flatMap(Optional::stream).toList();
        if (files.isEmpty()) {
            return BuildReport.missing();
        }
        ReportFile combined = new ReportFile(testReportNames(), newest(files));
        try {
            TestResults results = files.stream().map(file -> parse(file, JunitXmlReport::read))
                    .reduce(TestResults.none(), TestResults::plus);
            return BuildReport.read(results, combined);
        } catch (UnusableReport unreadable) {
            return BuildReport.unreadable(combined);
        }
    }

    private static <T> BuildReport<T> readOne(StampedFile file, Function<InputStream, T> reader) {
        try {
            return BuildReport.read(parse(file, reader), file.reportFile());
        } catch (UnusableReport unreadable) {
            return BuildReport.unreadable(file.reportFile());
        }
    }

    private static <T> T parse(StampedFile file, Function<InputStream, T> reader) {
        if (file.size() > MAXIMUM_BYTES) {
            throw unreadable(file, "It is larger than 50 MB.");
        }
        try (InputStream input = Files.newInputStream(file.path())) {
            return reader.apply(input);
        } catch (UnreadableReportException exception) {
            throw unreadable(file, exception.getMessage());
        } catch (IOException | UncheckedIOException exception) {
            throw unreadable(file, "Reading it failed with " + exception.getClass().getName() + ".");
        }
    }

    private static UnusableReport unreadable(StampedFile file, String reason) {
        LOGGER.warn("The build report " + file.reportFile().name() + " was not used: " + reason
                + " The rules that need it are skipped.");
        return new UnusableReport();
    }

    private Optional<Path> vulnerabilityReport() {
        return locations.vulnerabilities().stream().filter(Files::isRegularFile).findFirst();
    }

    private List<Path> testReports() {
        return locations.testResults().stream().flatMap(BuildReportFiles::testReportsIn).toList();
    }

    // Sorted, so that test results and their failed tests always come out in the same order.
    private static Stream<Path> testReportsIn(Path location) {
        if (!Files.isDirectory(location)) {
            return Stream.of(location).filter(Files::isRegularFile);
        }
        try (Stream<Path> children = Files.list(location)) {
            return children.filter(BuildReportFiles::isTestReport).sorted().toList().stream();
        } catch (IOException exception) {
            return Stream.empty();
        }
    }

    private static boolean isTestReport(Path file) {
        String name = file.getFileName().toString();
        return name.startsWith(TEST_REPORT_PREFIX) && name.endsWith(TEST_REPORT_SUFFIX) && Files.isRegularFile(file);
    }

    private String testReportNames() {
        return locations.testResults().stream()
                .filter(location -> testReportsIn(location).findAny().isPresent())
                .map(location -> location.getFileName().toString())
                .collect(Collectors.joining(", "));
    }

    private static Instant newest(List<StampedFile> files) {
        return files.stream().map(file -> file.reportFile().modifiedAt()).max(Comparator.naturalOrder()).orElseThrow();
    }

    /** Thrown, already logged, when one report cannot be used. */
    private static final class UnusableReport extends RuntimeException {

        @Serial
        private static final long serialVersionUID = 1L;

        UnusableReport() {
            super(null, null, false, false);
        }
    }
}
