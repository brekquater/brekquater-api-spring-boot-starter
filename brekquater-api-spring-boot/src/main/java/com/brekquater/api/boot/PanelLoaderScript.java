// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The few lines of JavaScript appended to Swagger UI's initializer. They add the panel's stylesheet and script to
 * the page; everything else happens in {@code panel.js}.
 */
final class PanelLoaderScript {

    // The address is written into JavaScript, so only characters that need no escaping there are accepted.
    private static final Pattern SAFE_PATH = Pattern.compile("(/[A-Za-z0-9._~-]+)+");

    private static final String TEMPLATE = """

            ;(function () {
              var base = "%s";
              var style = document.createElement("link");
              style.rel = "stylesheet";
              style.href = base + "/panel.css";
              document.head.appendChild(style);
              var script = document.createElement("script");
              script.src = base + "/panel.js";
              document.head.appendChild(script);
            })();
            """;

    private PanelLoaderScript() {}

    /**
     * Returns the loader for the panel served under {@code panelPath}, or empty when the path contains characters
     * that are not safe to write into a script.
     */
    static Optional<String> forPanelAt(String panelPath) {
        return SAFE_PATH.matcher(panelPath).matches() ? Optional.of(TEMPLATE.formatted(panelPath)) : Optional.empty();
    }
}
