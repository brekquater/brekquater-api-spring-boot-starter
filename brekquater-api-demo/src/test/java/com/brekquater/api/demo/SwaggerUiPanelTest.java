// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "good"})
class SwaggerUiPanelTest {

    private static final int OK = 200;
    private static final int FOUND = 302;
    private static final String INITIALIZER = "/swagger-ui/swagger-initializer.js";

    @LocalServerPort
    private int port;

    @Test
    void addsThePanelLoaderToSwaggerUisInitializer() {
        String initializer = new DemoHttpClient(port).get(INITIALIZER).body();

        assertThat(initializer)
                .contains("var base = \"/brekquater/api\";")
                .contains("SwaggerUIBundle");
    }

    @Test
    void keepsSpringdocsOwnSwaggerUiConfiguration() {
        String initializer = new DemoHttpClient(port).get(INITIALIZER).body();

        // springdoc leaves Swagger UI's sample URL in place and overrides it with its own configUrl.
        assertThat(initializer).contains("\"configUrl\" : \"/v3/api-docs/swagger-config\"");
    }

    @Test
    void servesThePanelScriptAndStylesheet() {
        DemoHttpClient http = new DemoHttpClient(port);

        HttpResponse<String> script = http.get("/brekquater/api/panel.js");
        HttpResponse<String> stylesheet = http.get("/brekquater/api/panel.css");

        assertThat(script.statusCode()).isEqualTo(OK);
        assertThat(script.headers().firstValue("Content-Type")).hasValueSatisfying(type -> assertThat(type)
                .startsWith("text/javascript"));
        assertThat(script.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(script.body()).contains("/assessment");
        assertThat(stylesheet.statusCode()).isEqualTo(OK);
        assertThat(stylesheet.body()).contains(".bq-panel");
    }

    @Test
    void sendsTheStandalonePageToItsHtmlFile() {
        DemoHttpClient http = new DemoHttpClient(port);

        HttpResponse<String> redirect = http.get("/brekquater/api");
        HttpResponse<String> page = http.get("/brekquater/api/index.html");

        assertThat(redirect.statusCode()).isEqualTo(FOUND);
        assertThat(redirect.headers().firstValue("Location")).hasValueSatisfying(location -> assertThat(location)
                .endsWith("/brekquater/api/index.html"));
        assertThat(page.statusCode()).isEqualTo(OK);
        assertThat(page.body()).contains("id=\"brekquater-panel\"", "src=\"panel.js\"");
    }
}
