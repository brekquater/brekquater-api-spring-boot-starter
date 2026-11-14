// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.evidence;

import com.brekquater.api.rules.Advisory;
import com.brekquater.api.rules.CvssBand;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;

/**
 * OSV-Scanner v2 JSON ({@code osv-scanner scan --format json}). OSV-Scanner groups the advisories that are aliases
 * of each other, such as a GHSA and its CVE, and gives each group a {@code max_severity}: the highest CVSS base
 * score as a string, or an empty string when no advisory in the group has one. Each group becomes one advisory.
 */
final class OsvScannerFormat implements VulnerabilityFormat {

    private static final String CVE_PREFIX = "CVE-";
    // OSV-Scanner writes the advisories with protobuf's JSON names, but older versions used the OSV schema's names.
    private static final List<String> DATABASE_SPECIFIC = List.of("database_specific", "databaseSpecific");

    @Override
    public boolean recognises(JsonNode report) {
        return report.path("results").isArray();
    }

    @Override
    public List<Advisory> advisories(JsonNode report) {
        return report.path("results").values().stream()
                .flatMap(result -> result.path("packages").values().stream())
                .flatMap(entry -> entry.path("groups").values().stream().map(group -> advisory(entry, group)))
                .toList();
    }

    private static Advisory advisory(JsonNode entry, JsonNode group) {
        List<String> ids = strings(group.path("ids"));
        List<JsonNode> records = entry.path("vulnerabilities").values().stream()
                .filter(record -> ids.contains(record.path("id").stringValue("")))
                .toList();
        Optional<BigDecimal> score = score(group.path("max_severity").stringValue(""));
        CvssBand band = score.map(CvssBand::of).or(() -> namedBand(records)).orElse(UNSCORED);
        return new Advisory(preferredId(group), packageUrl(entry.path("package")), band, score, fixed(entry, records));
    }

    private static Optional<BigDecimal> score(String maxSeverity) {
        try {
            return maxSeverity.isBlank() ? Optional.empty() : Optional.of(new BigDecimal(maxSeverity.trim()));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    // A GitHub advisory without a CVSS vector still names its severity, such as "MODERATE".
    private static Optional<CvssBand> namedBand(List<JsonNode> records) {
        return records.stream()
                .flatMap(record -> DATABASE_SPECIFIC.stream().map(name -> record.path(name).path("severity")))
                .flatMap(severity -> CvssBand.named(severity.stringValue("")).stream())
                .min(Comparator.naturalOrder());
    }

    // A CVE ID is what most developers search for, so it is shown when the group has one.
    private static String preferredId(JsonNode group) {
        List<String> ids = strings(group.path("ids"));
        return Stream.concat(ids.stream(), strings(group.path("aliases")).stream())
                .filter(id -> id.startsWith(CVE_PREFIX))
                .sorted()
                .findFirst()
                .orElseGet(() -> ids.stream().sorted().findFirst().orElse(""));
    }

    private static String packageUrl(JsonNode dependency) {
        String purl = dependency.path("purl").stringValue("");
        if (!purl.isBlank()) {
            return purl;
        }
        return dependency.path("name").stringValue("") + "@" + dependency.path("version").stringValue("");
    }

    // An advisory can fix several release lines, such as 2.3.2, 2.12.4 and 2.17.1. The one that applies is the
    // lowest fixed version above the installed one.
    private static Optional<String> fixed(JsonNode entry, List<JsonNode> records) {
        String name = entry.path("package").path("name").stringValue("");
        ReleaseNumber installed = ReleaseNumber.of(entry.path("package").path("version").stringValue(""));
        return records.stream()
                .flatMap(record -> record.path("affected").values().stream())
                .filter(affected -> affected.path("package").path("name").stringValue("").equals(name))
                .flatMap(affected -> affected.path("ranges").values().stream())
                .flatMap(range -> range.path("events").values().stream())
                .map(event -> event.path("fixed").stringValue(""))
                .filter(version -> !version.isBlank() && ReleaseNumber.of(version).compareTo(installed) > 0)
                .min(Comparator.comparing(ReleaseNumber::of));
    }

    private static List<String> strings(JsonNode array) {
        return array.values().stream().map(node -> node.stringValue("")).filter(id -> !id.isBlank()).toList();
    }
}
