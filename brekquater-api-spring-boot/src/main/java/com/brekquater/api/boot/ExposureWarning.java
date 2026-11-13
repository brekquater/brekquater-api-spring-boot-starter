// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Warns once at startup when API documentation is reachable outside a developer machine. Swagger UI and the
 * assessment both describe the whole API surface, which helps an attacker as much as a client developer.
 */
final class ExposureWarning implements ApplicationListener<ApplicationReadyEvent> {

    private static final Log LOGGER = LogFactory.getLog(ExposureWarning.class);
    private static final Profiles DEVELOPER_MACHINE = Profiles.of("dev | local");
    private static final String SWAGGER_UI_ENABLED = "springdoc.swagger-ui.enabled";

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment environment = event.getApplicationContext().getEnvironment();
        if (environment.acceptsProfiles(DEVELOPER_MACHINE)) {
            return;
        }
        List<String> exposed = exposedIn(environment);
        if (!exposed.isEmpty()) {
            LOGGER.warn(String.join(" ", exposed) + " Do this only on purpose, never on an environment that "
                    + "untrusted people can reach.");
        }
    }

    private static List<String> exposedIn(Environment environment) {
        List<String> exposed = new ArrayList<>();
        if (environment.getProperty(SWAGGER_UI_ENABLED, Boolean.class, true)) {
            exposed.add("Swagger UI is enabled outside the dev and local profiles; set " + SWAGGER_UI_ENABLED
                    + "=false to turn it off.");
        }
        if (PanelExposure.modeIn(environment) == PanelMode.PUBLIC) {
            exposed.add("The Brekquater API assessment is public outside the dev and local profiles; set "
                    + PanelExposure.MODE_PROPERTY + " to disabled or authenticated.");
        }
        return exposed;
    }
}
