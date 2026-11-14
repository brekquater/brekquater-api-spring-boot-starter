// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/** Strict mode as a team would use it in CI: the real application, started with brekquater.api.strict=true. */
class StrictModeTest {

    @Test
    void stopsTheBadApiBeforeItServesARequest() {
        Throwable failure = catchThrowable(() -> start("bad").close());

        assertThat(failure).hasStackTraceContaining("Strict mode stopped startup").hasStackTraceContaining("BQ-SEC-001");
    }

    @Test
    void startsTheGoodApi() {
        try (ConfigurableApplicationContext context = start("good")) {
            assertThat(context.isActive()).isTrue();
        }
    }

    private static ConfigurableApplicationContext start(String api) {
        return new SpringApplicationBuilder(DemoApplication.class)
                .profiles("dev", api)
                .properties(
                        "server.port=0",
                        "brekquater.api.strict=true",
                        // The good API is checked against passing sample reports, not the build still in progress.
                        "brekquater.api.reports.coverage=samples/" + api + "/jacoco.xml",
                        "brekquater.api.reports.test-results=samples/" + api,
                        "brekquater.api.reports.vulnerabilities=samples/" + api + "/osv-scanner.json")
                .run();
    }
}
