// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.evidence;

import com.brekquater.api.rules.Advisory;
import com.brekquater.api.rules.CvssBand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import tools.jackson.databind.JsonNode;

/**
 * OWASP Dependency-Check JSON ({@code -Dformats=JSON}, report schema 1.1). The score is the newest CVSS version
 * present: v4, then v3, then v2. Suppressed vulnerabilities are listed separately by Dependency-Check and are not
 * read, so its own suppression file keeps working.
 */
final class DependencyCheckFormat implements VulnerabilityFormat {

    private static final String PACKAGE_URL_PREFIX = "pkg:";
    private static final String MATCHED = "true";

    @Override
    public boolean recognises(JsonNode report) {
        return report.has("reportSchema") && report.path("dependencies").isArray();
    }

    @Override
    public List<Advisory> advisories(JsonNode report) {
        return report.path("dependencies").values().stream()
                .flatMap(dependency -> dependency.path("vulnerabilities").values().stream()
                        .map(vulnerability -> advisory(packageUrl(dependency), vulnerability)))
                .toList();
    }

    private static Advisory advisory(String packageUrl, JsonNode vulnerability) {
        Optional<BigDecimal> score = score(vulnerability);
        CvssBand band = score.map(CvssBand::of)
                .or(() -> CvssBand.named(vulnerability.path("severity").stringValue("")))
                .orElse(UNSCORED);
        String id = vulnerability.path("name").stringValue("");
        return new Advisory(id, packageUrl, band, score, fixedVersion(vulnerability));
    }

    private static Optional<BigDecimal> score(JsonNode vulnerability) {
        return Stream.of(
                        vulnerability.path("cvssv4").path("baseScore"),
                        vulnerability.path("cvssv3").path("baseScore"),
                        vulnerability.path("cvssv2").path("score"))
                .filter(JsonNode::isNumber)
                .map(JsonNode::decimalValue)
                .findFirst();
    }

    // The Package URL is the dependency's stable name; the jar file name is the fallback for files without one.
    private static String packageUrl(JsonNode dependency) {
        return dependency.path("packages").values().stream()
                .map(identifier -> identifier.path("id").stringValue(""))
                .filter(id -> id.startsWith(PACKAGE_URL_PREFIX))
                .findFirst()
                .orElseGet(() -> dependency.path("fileName").stringValue(""));
    }

    // The matched vulnerable-software range ends just before the first fixed version.
    private static Optional<String> fixedVersion(JsonNode vulnerability) {
        return vulnerability.path("vulnerableSoftware").values().stream()
                .map(entry -> entry.path("software"))
                .filter(software -> software.path("vulnerabilityIdMatched").stringValue("").equals(MATCHED))
                .map(software -> software.path("versionEndExcluding").stringValue(""))
                .filter(version -> !version.isBlank())
                .findFirst();
    }
}
