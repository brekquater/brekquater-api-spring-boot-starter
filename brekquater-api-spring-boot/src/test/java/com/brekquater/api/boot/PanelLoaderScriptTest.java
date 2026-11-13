// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class PanelLoaderScriptTest {

    @Test
    void loadsThePanelsStylesheetAndScriptFromThePanelPath() {
        assertThat(PanelLoaderScript.forPanelAt("/shop/brekquater/api"))
                .hasValueSatisfying(script -> assertThat(script)
                        .contains("var base = \"/shop/brekquater/api\";")
                        .contains("base + \"/panel.css\"")
                        .contains("base + \"/panel.js\"")
                        .startsWith("\n;"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/", "brekquater/api", "/a\"b", "/a b", "/a</script>", "/a\\b", "//api"})
    void refusesAPathThatIsNotSafeToWriteIntoAScript(String path) {
        assertThat(PanelLoaderScript.forPanelAt(path)).isEmpty();
    }
}
