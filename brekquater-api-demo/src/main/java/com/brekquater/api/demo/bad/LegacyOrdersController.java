// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo.bad;

import com.brekquater.api.demo.Order;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * An orders API with the mistakes the rules look for. Each method is commented with the rules it breaks. Do not
 * copy it.
 */
@Profile("bad")
@RestController
class LegacyOrdersController {

    private static final List<Order> ORDERS = List.of(new Order("1001", "SHIPPED"));

    // BQ-NAM-002 trailing slash, BQ-NAM-003 CRUD verb, BQ-DES-001 unpaginated list, BQ-DOC-001 no summary,
    // BQ-ERR-001 no error response, BQ-SEC-001 no security, BQ-API-002 together with LegacyCustomersController.list.
    @GetMapping("/getOrders/")
    List<Order> list() {
        return ORDERS;
    }

    // BQ-NAM-001 camelCase segment, BQ-API-005 free-form response object.
    @GetMapping("/orderItems/{id}")
    Map<String, Object> orderItem(@PathVariable String id) {
        return Map.of("id", id);
    }

    // BQ-DES-002 body on GET. BQ-ERR-002 error not sent as problem details. BQ-SEC-002 undefined scheme "jwt".
    @GetMapping("/orders/search")
    @SecurityRequirement(name = "jwt")
    @ApiResponse(
            responseCode = "400",
            description = "Bad query",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LegacyError.class)))
    List<Order> search(@RequestBody OrderQuery query) {
        return ORDERS;
    }

    /**
     * Search criteria sent in the body of a GET.
     *
     * @param status the status to look for
     */
    record OrderQuery(String status) {}

    /**
     * A home-grown error body instead of problem details.
     *
     * @param error what went wrong
     */
    record LegacyError(String error) {}
}
