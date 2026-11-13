// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "good"})
class GoodApiTest {

    private static final int OK = 200;

    @LocalServerPort
    private int port;

    @Test
    void servesTheOrdersApiAndSwaggerUi() {
        DemoHttpClient http = new DemoHttpClient(port);

        assertThat(http.get("/orders?page=0&size=10").body()).contains("SHIPPED");
        assertThat(http.get("/swagger-ui/index.html").statusCode()).isEqualTo(OK);
    }

    @Test
    void passesEveryRule() {
        JsonNode assessment = new DemoHttpClient(port).assessment();

        assertThat(assessment.get("status").asString()).isEqualTo("ASSESSED");
        assertThat(assessment.get("findings")).isEmpty();
        assertThat(assessment.get("failedRules")).isEmpty();
        assertThat(assessment.get("score").get("overall").asInt()).isEqualTo(100);
        assertThat(assessment.get("securityRating").asString()).isEqualTo("A");
        assertThat(assessment.get("document").get("title").asString()).isEqualTo("Orders API");
    }

    @Test
    void keepsTheAssessmentEndpointOutOfTheApplicationsOwnDocument() {
        assertThat(new DemoHttpClient(port).get("/v3/api-docs").body())
                .contains("\"/orders\"")
                .doesNotContain("brekquater");
    }
}
