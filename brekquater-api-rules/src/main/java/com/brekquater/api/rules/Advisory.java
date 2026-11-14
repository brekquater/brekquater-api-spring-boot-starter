// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * A known vulnerability in one dependency, as a scanner reported it.
 *
 * @param id the advisory ID, preferably a CVE ID, otherwise for example a GHSA ID
 * @param packageUrl the affected dependency, as a Package URL such as {@code pkg:maven/org.yaml/snakeyaml@1.33}
 *     when the scanner gives one
 * @param band the severity band
 * @param score the CVSS base score; empty when the scanner gave only a band, or no severity at all
 * @param fixedVersion the first version that is not affected, when the scanner knows it
 */
public record Advisory(
        String id, String packageUrl, CvssBand band, Optional<BigDecimal> score, Optional<String> fixedVersion) {

    /** Order of advisories in every report: by dependency, then by advisory ID. */
    public static final Comparator<Advisory> REPORT_ORDER =
            Comparator.comparing(Advisory::packageUrl).thenComparing(Advisory::id);

    public Advisory {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(packageUrl, "packageUrl");
        Objects.requireNonNull(band, "band");
        Objects.requireNonNull(score, "score");
        Objects.requireNonNull(fixedVersion, "fixedVersion");
    }
}
