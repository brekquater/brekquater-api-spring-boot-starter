// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring's path-segment API versioning reads a version from every request path. Without care, that turns every
 * request for the panel into a 400, because "brekquater" is not a version.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"spring.mvc.apiversion.use.path-segment=0", "spring.mvc.apiversion.required=true"})
@ActiveProfiles({"dev", "good"})
class ApiVersioningTest {

    private static final int OK = 200;
    private static final int BAD_REQUEST = 400;

    @LocalServerPort
    private int port;

    @Test
    void servesThePanelAndTheAssessmentWhenEveryPathCarriesAVersion() {
        DemoHttpClient http = new DemoHttpClient(port);

        assertThat(http.get("/brekquater/api/assessment").statusCode()).isEqualTo(OK);
        assertThat(http.get("/brekquater/api/panel.js").statusCode()).isEqualTo(OK);
        assertThat(http.get("/brekquater/api/index.html").statusCode()).isEqualTo(OK);
    }

    @Test
    void leavesVersioningOfTheApplicationsOwnPathsAlone() {
        assertThat(new DemoHttpClient(port).get("/orders").statusCode()).isEqualTo(BAD_REQUEST);
    }
}
