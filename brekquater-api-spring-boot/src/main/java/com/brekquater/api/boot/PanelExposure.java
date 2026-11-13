// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Decides who may see the assessment. An explicit {@code brekquater.api.panel.mode} always wins. Without one, the
 * assessment is public on a developer machine, recognised by the {@code dev} or {@code local} profile, and
 * disabled everywhere else, so a production deployment never exposes its API surface by accident.
 */
final class PanelExposure {

    static final String MODE_PROPERTY = "brekquater.api.panel.mode";

    private static final Profiles DEVELOPER_MACHINE = Profiles.of("dev | local");

    private PanelExposure() {}

    static PanelMode modeIn(Environment environment) {
        return Binder.get(environment)
                .bind(MODE_PROPERTY, PanelMode.class)
                .orElseGet(() -> defaultModeIn(environment));
    }

    private static PanelMode defaultModeIn(Environment environment) {
        return environment.acceptsProfiles(DEVELOPER_MACHINE) ? PanelMode.PUBLIC : PanelMode.DISABLED;
    }
}
