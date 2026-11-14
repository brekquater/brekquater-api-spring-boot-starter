// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.brekquater.api.rules.ClassCoverage;
import com.brekquater.api.rules.Coverage;
import com.brekquater.api.rules.CoverageCounter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class JacocoXmlReportTest {

    // Shaped like the report jacoco-maven-plugin 0.8.16 writes, including its DOCTYPE.
    private static final String REPORT = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>\
            <!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd">\
            <report name="Orders">\
            <sessioninfo id="host-1" start="1" dump="2"/>\
            <package name="com/example/orders">\
            <class name="com/example/orders/OrderService" sourcefilename="OrderService.java">\
            <method name="find" desc="()V" line="10"><counter type="LINE" missed="1" covered="3"/></method>\
            <counter type="INSTRUCTION" missed="9" covered="40"/>\
            <counter type="BRANCH" missed="1" covered="7"/>\
            <counter type="LINE" missed="5" covered="15"/>\
            </class>\
            <class name="com/example/orders/OrdersController" sourcefilename="OrdersController.java">\
            <counter type="LINE" missed="0" covered="10"/>\
            </class>\
            <sourcefile name="OrderService.java"><line nr="10" mi="0" ci="3" mb="0" cb="0"/>\
            <counter type="LINE" missed="5" covered="15"/></sourcefile>\
            <counter type="BRANCH" missed="9" covered="9"/>\
            <counter type="LINE" missed="9" covered="99"/>\
            </package>\
            <counter type="BRANCH" missed="2" covered="2"/>\
            <counter type="LINE" missed="5" covered="25"/>\
            </report>""";

    @Test
    void readsTheProjectTotalsFromTheCountersOfTheReportItself() {
        Coverage coverage = JacocoXmlReport.read(stream(REPORT));

        assertThat(coverage.lines()).isEqualTo(new CoverageCounter(5, 25));
        assertThat(coverage.branches()).isEqualTo(new CoverageCounter(2, 2));
    }

    @Test
    void readsTheLineCoverageOfEachClassWithDottedNames() {
        Coverage coverage = JacocoXmlReport.read(stream(REPORT));

        assertThat(coverage.classes())
                .containsExactly(
                        new ClassCoverage("com.example.orders.OrderService", new CoverageCounter(5, 15)),
                        new ClassCoverage("com.example.orders.OrdersController", new CoverageCounter(0, 10)));
    }

    @Test
    void treatsAMissingBranchCounterAsAProjectWithoutBranches() {
        Coverage coverage = JacocoXmlReport.read(stream("""
                <report name="Orders"><counter type="LINE" missed="0" covered="4"/></report>"""));

        assertThat(coverage.branches().total()).isZero();
    }

    @Test
    void refusesAFileThatIsNotAJacocoReport() {
        assertThatExceptionOfType(UnreadableReportException.class)
                .isThrownBy(() -> JacocoXmlReport.read(stream("<testsuite name=\"x\"/>")))
                .withMessageContaining("not a JaCoCo XML report");
    }

    @Test
    void refusesACounterWithoutCounts() {
        assertThatExceptionOfType(UnreadableReportException.class)
                .isThrownBy(() -> JacocoXmlReport.read(stream("<report><counter type=\"LINE\"/></report>")));
    }

    @Test
    void neverResolvesAnExternalEntity() {
        String attack = """
                <?xml version="1.0"?>
                <!DOCTYPE report [<!ENTITY secret SYSTEM "file:///etc/hostname">]>
                <report name="&secret;"><counter type="LINE" missed="0" covered="1"/></report>""";

        assertThatExceptionOfType(UnreadableReportException.class).isThrownBy(() -> JacocoXmlReport.read(stream(attack)));
    }

    @Test
    void refusesXmlThatIsNotWellFormed() {
        assertThatExceptionOfType(UnreadableReportException.class)
                .isThrownBy(() -> JacocoXmlReport.read(stream("<report>")))
                .withMessageContaining("not well-formed");
    }

    private static InputStream stream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}
