// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.evidence;

import java.util.Arrays;
import java.util.List;

/**
 * The numeric part of a version, such as 2.17.1, ordered number by number. An advisory can list several fixed
 * versions, one per release line, and this picks the one that applies to the installed version.
 *
 * <p>Qualifiers after the numbers, such as {@code -beta9} or {@code .Final}, are ignored, so {@code 2.0-beta9} and
 * {@code 2.0} are equal. That is precise enough to choose a fixed release, which is all it is used for.
 */
final class ReleaseNumber implements Comparable<ReleaseNumber> {

    private static final String ZERO = "0";

    private final List<String> numbers;

    private ReleaseNumber(List<String> numbers) {
        this.numbers = numbers;
    }

    static ReleaseNumber of(String version) {
        String numericPart = version.trim().replaceFirst("[^0-9.].*$", "");
        return new ReleaseNumber(Arrays.stream(numericPart.split("\\."))
                .filter(number -> !number.isEmpty())
                .map(ReleaseNumber::withoutLeadingZeros)
                .toList());
    }

    @Override
    public int compareTo(ReleaseNumber other) {
        int length = Math.max(numbers.size(), other.numbers.size());
        for (int index = 0; index < length; index++) {
            int order = compareNumbers(numberAt(index), other.numberAt(index));
            if (order != 0) {
                return order;
            }
        }
        return 0;
    }

    private String numberAt(int index) {
        return index < numbers.size() ? numbers.get(index) : ZERO;
    }

    // Compared as text, so numbers of any length work without overflowing.
    private static int compareNumbers(String left, String right) {
        return left.length() != right.length() ? Integer.compare(left.length(), right.length()) : left.compareTo(right);
    }

    private static String withoutLeadingZeros(String number) {
        String stripped = number.replaceFirst("^0+", "");
        return stripped.isEmpty() ? ZERO : stripped;
    }
}
