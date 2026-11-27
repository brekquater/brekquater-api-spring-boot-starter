// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo.bad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * A reports API that grew several versions without a plan. Each method is commented with the rules it breaks. Do
 * not copy it.
 */
@Profile("bad")
@RestController
class LegacyReportsController {

    // BQ-DES-017: version 1 is older than version 2 but not deprecated.
    @GetMapping("/v1/reports")
    List<String> listV1() {
        return List.of();
    }

    @GetMapping("/v2/reports")
    List<String> listV2() {
        return List.of();
    }

    // BQ-NAM-007: a minor version in the path. BQ-DES-016: the version is not the first segment, as it is above.
    @GetMapping("/api/v2.1/reports/{id}")
    String findV21(@PathVariable String id) {
        return id;
    }

    // BQ-DES-015: a version header where the other operations use the path.
    @GetMapping("/reports/latest")
    String latest(@RequestHeader("X-API-Version") String version) {
        return version;
    }

    // BQ-DOC-012: deprecated without saying what replaces it. BQ-DOC-013: the sunset is not a date.
    @Operation(deprecated = true, extensions = @Extension(properties = @ExtensionProperty(name = "x-sunset", value = "soon")))
    @GetMapping("/v1/reports/summary")
    String summary() {
        return "";
    }
}
