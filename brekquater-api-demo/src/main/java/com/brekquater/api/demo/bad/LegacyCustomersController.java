// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo.bad;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Has a method named like one in {@link LegacyOrdersController}, so springdoc gives one of them a numeric
 * operationId suffix (BQ-API-002).
 */
@Profile("bad")
@RestController
class LegacyCustomersController {

    @GetMapping("/customers")
    List<String> list() {
        return List.of("Ada", "Grace");
    }
}
