// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.evidence;

import com.brekquater.api.rules.ClassCoverage;
import com.brekquater.api.rules.Coverage;
import com.brekquater.api.rules.CoverageCounter;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import javax.xml.stream.XMLStreamReader;

/**
 * Reads a JaCoCo XML report ({@code jacoco.xml}). Only counters that are direct children of {@code report} give the
 * project totals; those under a {@code class} give that class's line coverage. Counters of packages, methods and
 * source files are ignored, so nothing is counted twice.
 */
public final class JacocoXmlReport {

    private JacocoXmlReport() {}

    public static Coverage read(InputStream input) {
        CoverageCollector collector = new CoverageCollector();
        HardenedXml.walk(input, collector);
        return collector.coverage();
    }

    private static final class CoverageCollector implements HardenedXml.ElementHandler {

        private static final String REPORT = "report";
        private static final String CLASS = "class";
        private static final String COUNTER = "counter";
        private static final String LINE = "LINE";
        private static final String BRANCH = "BRANCH";
        // JaCoCo leaves out a counter whose total is zero, so a project without branches has no BRANCH counter.
        private static final CoverageCounter NOTHING = new CoverageCounter(0, 0);

        private final Deque<String> openElements = new ArrayDeque<>();
        private final List<ClassCoverage> classes = new ArrayList<>();
        private String className = "";
        private CoverageCounter lines = NOTHING;
        private CoverageCounter branches = NOTHING;

        @Override
        public void start(XMLStreamReader element) {
            String name = element.getLocalName();
            if (openElements.isEmpty() && !name.equals(REPORT)) {
                throw new UnreadableReportException("The file is not a JaCoCo XML report.");
            }
            if (name.equals(CLASS)) {
                className = attribute(element, "name").replace('/', '.');
            } else if (name.equals(COUNTER)) {
                count(element, openElements.peek());
            }
            openElements.push(name);
        }

        @Override
        public void end(String name) {
            openElements.pop();
        }

        Coverage coverage() {
            return new Coverage(lines, branches, classes);
        }

        private void count(XMLStreamReader element, String parent) {
            String type = attribute(element, "type");
            CoverageCounter counter = counter(element);
            if (parent.equals(CLASS) && type.equals(LINE)) {
                classes.add(new ClassCoverage(className, counter));
            } else if (parent.equals(REPORT) && type.equals(LINE)) {
                lines = counter;
            } else if (parent.equals(REPORT) && type.equals(BRANCH)) {
                branches = counter;
            }
        }

        private static CoverageCounter counter(XMLStreamReader element) {
            try {
                return new CoverageCounter(
                        Integer.parseInt(attribute(element, "missed")), Integer.parseInt(attribute(element, "covered")));
            } catch (IllegalArgumentException exception) {
                throw new UnreadableReportException("A JaCoCo counter has no valid missed or covered count.", exception);
            }
        }

        private static String attribute(XMLStreamReader element, String name) {
            return Optional.ofNullable(element.getAttributeValue(null, name)).orElse("");
        }
    }
}
