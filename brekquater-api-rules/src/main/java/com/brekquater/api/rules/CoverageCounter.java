// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

/**
 * How many items of one kind, such as lines or branches, the tests reached.
 *
 * @param missed items no test reached
 * @param covered items at least one test reached
 */
public record CoverageCounter(int missed, int covered) {

    private static final int PERCENT = 100;

    public CoverageCounter {
        if (missed < 0 || covered < 0) {
            throw new IllegalArgumentException("Counts must not be negative: missed=" + missed + ", covered=" + covered);
        }
    }

    public int total() {
        return missed + covered;
    }

    /** Returns the covered share in whole percent, rounded down so that the number never overstates coverage. */
    public int percent() {
        return total() == 0 ? PERCENT : (int) ((long) covered * PERCENT / total());
    }

    /** Returns whether the covered share reaches {@code targetPercent}, compared exactly rather than rounded. */
    public boolean meets(int targetPercent) {
        return (long) covered * PERCENT >= (long) targetPercent * total();
    }
}
