// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo.bad;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A catalogue API whose schemas contradict themselves. Each parameter and field is commented with the rule it
 * breaks. Do not copy it.
 */
@Profile("bad")
@RestController
class LegacyCatalogueController {

    private static final LegacyProduct PRODUCT = new LegacyProduct(1, 1, 1, "a", "a", "http://a", "http://b", "x");

    // BQ-DOC-010: "first" is not a page number.
    @GetMapping("/products")
    LegacyProduct find(@Parameter(example = "first") @RequestParam(required = false) Integer page) {
        return PRODUCT;
    }

    /**
     * A product as the legacy API describes it.
     *
     * @param stock BQ-DOC-009: the example is not a number
     * @param added BQ-DES-010: a date-time that is an integer
     * @param size BQ-DES-011: string values for an integer
     * @param colour BQ-DES-012: "red" is listed twice
     * @param status BQ-DOC-009: the default is not one of the values
     * @param url BQ-NAM-006: differs only in case from URL
     * @param otherUrl BQ-NAM-006: written as URL
     * @param extra BQ-DES-014: anything at all
     */
    @Schema(discriminatorProperty = "kind") // BQ-DES-013: no product has a kind.
    record LegacyProduct(
            @Schema(example = "many") int stock,
            @Schema(format = "date-time") int added,
            @Schema(allowableValues = {"1", "2"}) int size,
            @Schema(allowableValues = {"red", "red"}) String colour,
            @Schema(allowableValues = {"on", "off"}, defaultValue = "paused") String status,
            String url,
            @JsonProperty("URL") String otherUrl,
            Object extra) {}
}
