// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.evidence;

import com.brekquater.api.rules.FailedTest;
import com.brekquater.api.rules.TestResults;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.xml.stream.XMLStreamReader;

/**
 * Reads one JUnit XML report, the {@code TEST-*.xml} format that Surefire and Failsafe write. Test cases are counted
 * from the {@code testcase} elements rather than the suite's summary attributes, so a test that failed and passed
 * on a rerun ({@code flakyFailure}) counts as passed, as Surefire itself treats it.
 */
public final class JunitXmlReport {

    private JunitXmlReport() {}

    public static TestResults read(InputStream input) {
        TestCaseCollector collector = new TestCaseCollector();
        HardenedXml.walk(input, collector);
        return collector.results();
    }

    private static final class TestCaseCollector implements HardenedXml.ElementHandler {

        private static final Set<String> ROOTS = Set.of("testsuite", "testsuites");
        private static final String TEST_CASE = "testcase";
        private static final String SKIPPED = "skipped";
        private static final Set<String> FAILED = Set.of("failure", "error");

        private final List<FailedTest> failed = new ArrayList<>();
        private boolean rootSeen;
        private int tests;
        private int skipped;
        private Optional<FailedTest> current = Optional.empty();
        private boolean currentFailed;
        private boolean currentSkipped;

        @Override
        public void start(XMLStreamReader element) {
            String name = element.getLocalName();
            requireRoot(name);
            if (name.equals(TEST_CASE)) {
                current = Optional.of(new FailedTest(attribute(element, "classname"), attribute(element, "name")));
                currentFailed = false;
                currentSkipped = false;
            } else if (current.isPresent()) {
                currentFailed |= FAILED.contains(name);
                currentSkipped |= name.equals(SKIPPED);
            }
        }

        @Override
        public void end(String name) {
            if (name.equals(TEST_CASE)) {
                current.ifPresent(this::count);
                current = Optional.empty();
            }
        }

        TestResults results() {
            return new TestResults(tests, skipped, failed);
        }

        private void requireRoot(String name) {
            if (!rootSeen && !ROOTS.contains(name)) {
                throw new UnreadableReportException("The file is not a JUnit XML test report.");
            }
            rootSeen = true;
        }

        private void count(FailedTest testCase) {
            tests++;
            if (currentFailed) {
                failed.add(testCase);
            } else if (currentSkipped) {
                skipped++;
            }
        }

        private static String attribute(XMLStreamReader element, String name) {
            return Optional.ofNullable(element.getAttributeValue(null, name)).orElse("");
        }
    }
}
