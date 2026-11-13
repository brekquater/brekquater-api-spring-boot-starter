// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(OutputCaptureExtension.class)
class ExposureWarningTest {

    private static final String SWAGGER_UI = "Swagger UI is enabled outside the dev and local profiles";
    private static final String ASSESSMENT = "The Brekquater API assessment is public";

    @Test
    void warnsThatSwaggerUiIsOnInAnyOtherProfile(CapturedOutput output) {
        startWith(new MockEnvironment().withProperty("spring.profiles.active", "prod"));

        assertThat(output).contains(SWAGGER_UI).doesNotContain(ASSESSMENT);
    }

    @Test
    void warnsThatAPublicAssessmentIsOnInAnyOtherProfile(CapturedOutput output) {
        startWith(new MockEnvironment()
                .withProperty("springdoc.swagger-ui.enabled", "false")
                .withProperty("brekquater.api.panel.mode", "public"));

        assertThat(output).contains(ASSESSMENT, "brekquater.api.panel.mode").doesNotContain(SWAGGER_UI);
    }

    @Test
    void staysQuietOnADeveloperMachine(CapturedOutput output) {
        MockEnvironment developerMachine = new MockEnvironment().withProperty("brekquater.api.panel.mode", "public");
        developerMachine.setActiveProfiles("local");

        startWith(developerMachine);

        assertThat(output).doesNotContain(SWAGGER_UI, ASSESSMENT);
    }

    @Test
    void staysQuietWhenNothingIsExposed(CapturedOutput output) {
        startWith(new MockEnvironment().withProperty("springdoc.swagger-ui.enabled", "false"));

        assertThat(output).doesNotContain(SWAGGER_UI, ASSESSMENT);
    }

    private static void startWith(MockEnvironment environment) {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.setEnvironment(environment);
            context.refresh();
            new ExposureWarning().onApplicationEvent(
                    new ApplicationReadyEvent(new SpringApplication(), new String[0], context, Duration.ZERO));
        }
    }
}
