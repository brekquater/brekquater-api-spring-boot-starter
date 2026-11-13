// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.RuleRegistry;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "bad"})
class BadApiTest {

    @LocalServerPort
    private int port;

    // springdoc renames duplicate operationIds after every customizer has run, so a springdoc application cannot
    // produce this finding; the duplicate shows up as BQ-API-002 instead. The rule's own tests cover it.
    private static final String UNREACHABLE_WITH_SPRINGDOC = "BQ-API-004";

    @Test
    void breaksEveryRuleThatASpringdocApplicationCanBreak() {
        JsonNode findings = new DemoHttpClient(port).assessment().get("findings");

        List<String> brokenRules = StreamSupport.stream(findings.spliterator(), false)
                .map(finding -> finding.get("ruleId").asString())
                .distinct()
                .toList();
        List<String> breakableRules = RuleRegistry.fromServiceLoader().ids().stream()
                .filter(ruleId -> !ruleId.equals(UNREACHABLE_WITH_SPRINGDOC))
                .toList();
        assertThat(brokenRules).containsExactlyInAnyOrderElementsOf(breakableRules);
    }

    @Test
    void flagsTheUnsecuredOperationsAsBlockers() {
        JsonNode assessment = new DemoHttpClient(port).assessment();

        assertThat(assessment.get("blockers").asInt()).isPositive();
        assertThat(assessment.get("securityRating").asString()).isEqualTo("E");
        assertThat(assessment.get("score").get("grade").asString()).isEqualTo("F");
    }

    @Test
    void everyFindingPointsAtTheDocumentAndLinksToItsRulePage() {
        JsonNode findings = new DemoHttpClient(port).assessment().get("findings");

        assertThat(findings).allSatisfy(finding -> {
            assertThat(finding.get("evidence").get("location").asString()).startsWith("/");
            assertThat(finding.get("docUrl").asString())
                    .isEqualTo("https://starter.brekquater.com/api/rules/" + finding.get("ruleId").asString());
        });
    }
}
