// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.List;

/**
 * The known vulnerabilities a scanner found in the application's dependencies. Advisories are kept in a fixed order
 * and without repeats, however the scanner listed them, so that the same report always gives the same findings.
 *
 * @param advisories one entry per advisory and affected dependency
 */
public record Vulnerabilities(List<Advisory> advisories) {

    public Vulnerabilities {
        advisories = advisories.stream().distinct().sorted().toList();
    }

    /** Returns the advisories in the given severity band. */
    public List<Advisory> in(CvssBand band) {
        return advisories.stream().filter(advisory -> advisory.band() == band).toList();
    }
}
