// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/** Without a developer profile the assessment must not be reachable at all. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"prod", "good"})
class ProductionProfileTest {

    private static final int NOT_FOUND = 404;

    @LocalServerPort
    private int port;

    @Test
    void doesNotServeTheAssessmentOrTheDocument() {
        DemoHttpClient http = new DemoHttpClient(port);

        assertThat(http.get("/brekquater/api/assessment").statusCode()).isEqualTo(NOT_FOUND);
        assertThat(http.get("/brekquater/api/openapi").statusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void doesNotServeThePanelOrItsStandalonePage() {
        DemoHttpClient http = new DemoHttpClient(port);

        assertThat(http.get("/brekquater/api/panel.js").statusCode()).isEqualTo(NOT_FOUND);
        assertThat(http.get("/brekquater/api/index.html").statusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void addsNothingToSwaggerUi() {
        assertThat(new DemoHttpClient(port).get("/swagger-ui/swagger-initializer.js").body())
                .contains("SwaggerUIBundle")
                .doesNotContain("brekquater");
    }
}
