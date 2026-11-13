// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Serves the panel's script and stylesheet, and the standalone page that shows the panel without Swagger UI. They
 * hold no data about the API; the data comes from the assessment endpoint, which checks the caller.
 *
 * <p>The files are kept outside the folders Spring Boot serves automatically, so they disappear together with the
 * endpoint when the panel is disabled.
 */
@Hidden
@Controller
@RequestMapping("${brekquater.api.panel.base-path:/brekquater/api}")
final class PanelPageController {

    private static final String ASSETS = "brekquater/panel/";
    private static final MediaType JAVASCRIPT = new MediaType("text", "javascript");
    private static final MediaType CSS = new MediaType("text", "css");
    private static final String PAGE = "/index.html";

    // The page refers to its script and stylesheet relatively, so it is served below the base path, not at it.
    @GetMapping
    ResponseEntity<Void> page() {
        return ResponseEntity.status(302)
                .location(ServletUriComponentsBuilder.fromCurrentRequestUri().path(PAGE).build().toUri())
                .build();
    }

    @GetMapping(PAGE)
    ResponseEntity<Resource> standalonePage() {
        return asset("index.html", MediaType.TEXT_HTML);
    }

    @GetMapping("/panel.js")
    ResponseEntity<Resource> script() {
        return asset("panel.js", JAVASCRIPT);
    }

    @GetMapping("/panel.css")
    ResponseEntity<Resource> stylesheet() {
        return asset("panel.css", CSS);
    }

    // "no-cache" makes browsers revalidate, so an upgraded starter never runs against a stale script.
    private static ResponseEntity<Resource> asset(String name, MediaType type) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(type)
                .header("X-Content-Type-Options", "nosniff")
                .body(new ClassPathResource(ASSETS + name, PanelPageController.class.getClassLoader()));
    }
}
