// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;
import java.util.stream.Stream;

/**
 * What the test reports say, summed over every report that was read.
 *
 * @param tests every test case in the reports, including skipped and failed ones
 * @param skipped test cases that did not run
 * @param failed test cases that failed or ended with an error
 */
public record TestResults(int tests, int skipped, List<FailedTest> failed) {

    private static final TestResults NONE = new TestResults(0, 0, List.of());

    public TestResults {
        failed = List.copyOf(failed);
        if (skipped < 0 || skipped + failed.size() > tests) {
            throw new IllegalArgumentException(
                    "Inconsistent counts: tests=" + tests + ", skipped=" + skipped + ", failed=" + failed.size());
        }
    }

    public static TestResults none() {
        return NONE;
    }

    /** Returns how many test cases ran, that is every test case except the skipped ones. */
    public int executed() {
        return tests - skipped;
    }

    public int passed() {
        return executed() - failed.size();
    }

    /** Returns these results and {@code other} as one, for example the unit and the integration tests. */
    public TestResults plus(TestResults other) {
        List<FailedTest> allFailed = Stream.concat(failed.stream(), other.failed.stream()).toList();
        return new TestResults(tests + other.tests, skipped + other.skipped, allFailed);
    }
}
