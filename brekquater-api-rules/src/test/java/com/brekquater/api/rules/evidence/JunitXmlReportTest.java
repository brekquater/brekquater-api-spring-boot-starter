// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.brekquater.api.rules.FailedTest;
import com.brekquater.api.rules.TestResults;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class JunitXmlReportTest {

    // Shaped like a Surefire 3.5 TEST-*.xml file. The suite's summary attributes are deliberately wrong, to show
    // that the test cases are what counts.
    private static final String REPORT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="com.example.OrdersTest" tests="99" errors="0" skipped="0" failures="0">
              <properties><property name="java.version" value="25"/></properties>
              <testcase name="listsOrders" classname="com.example.OrdersTest" time="0.1"/>
              <testcase name="findsAnOrder" classname="com.example.OrdersTest" time="0.1">
                <failure message="expected 1 but was 2" type="org.opentest4j.AssertionFailedError">trace</failure>
                <system-out>log</system-out>
              </testcase>
              <testcase name="savesAnOrder" classname="com.example.OrdersTest" time="0.1">
                <error type="java.lang.IllegalStateException">trace</error>
              </testcase>
              <testcase name="deletesAnOrder" classname="com.example.OrdersTest" time="0">
                <skipped message="not yet"/>
              </testcase>
              <testcase name="pagesOrders" classname="com.example.OrdersTest" time="0.2">
                <flakyFailure message="timeout" type="java.lang.AssertionError">trace</flakyFailure>
              </testcase>
            </testsuite>
            """;

    @Test
    void countsEveryTestCaseAndTheSkippedOnes() {
        TestResults results = JunitXmlReport.read(stream(REPORT));

        assertThat(results.tests()).isEqualTo(5);
        assertThat(results.skipped()).isEqualTo(1);
        assertThat(results.passed()).isEqualTo(2);
    }

    @Test
    void reportsFailuresAndErrorsAsFailedTests() {
        TestResults results = JunitXmlReport.read(stream(REPORT));

        assertThat(results.failed())
                .containsExactly(
                        new FailedTest("com.example.OrdersTest", "findsAnOrder"),
                        new FailedTest("com.example.OrdersTest", "savesAnOrder"));
    }

    @Test
    void countsATestThatPassedOnARerunAsPassed() {
        TestResults results = JunitXmlReport.read(stream(REPORT));

        assertThat(results.failed()).extracting(FailedTest::name).doesNotContain("pagesOrders");
    }

    @Test
    void acceptsSeveralSuitesInOneFile() {
        TestResults results = JunitXmlReport.read(stream("""
                <testsuites>
                  <testsuite name="a"><testcase name="one" classname="A"/></testsuite>
                  <testsuite name="b"><testcase name="two" classname="B"/></testsuite>
                </testsuites>
                """));

        assertThat(results.tests()).isEqualTo(2);
    }

    @Test
    void refusesAFileThatIsNotATestReport() {
        assertThatExceptionOfType(UnreadableReportException.class)
                .isThrownBy(() -> JunitXmlReport.read(stream("<report name=\"x\"/>")))
                .withMessageContaining("not a JUnit XML test report");
    }

    private static InputStream stream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}
