// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo.bad;

import com.brekquater.api.demo.Order;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * An orders API with the mistakes the rules look for. Each method is commented with the rules it breaks. Do not
 * copy it.
 */
@Profile("bad")
@RestController
// BQ-SEC-010: the resource owner password flow hands the user's password to the client.
@SecurityScheme(
        name = "legacyLogin",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(password = @OAuthFlow(tokenUrl = "https://auth.example.com/token")))
class LegacyOrdersController {

    private static final List<Order> ORDERS = List.of(new Order("1001", "SHIPPED"));

    // BQ-NAM-002 trailing slash, BQ-NAM-003 CRUD verb, BQ-DES-001 unpaginated list, BQ-DOC-001 no summary,
    // BQ-ERR-001 no error response, BQ-SEC-001 no security, BQ-API-002 together with LegacyCustomersController.list,
    // BQ-DOC-008 response documented as */*.
    @GetMapping("/getOrders/")
    List<Order> list() {
        return ORDERS;
    }

    // BQ-NAM-001 camelCase segment, BQ-API-005 free-form response object.
    @GetMapping("/orderItems/{id}")
    Map<String, Object> orderItem(@PathVariable String id) {
        return Map.of("id", id);
    }

    // BQ-DES-006: the same template as GET /orderItems/{id}, with another variable name.
    @PutMapping("/orderItems/{itemId}")
    Order replaceOrderItem(@PathVariable String itemId, @RequestBody Order item) {
        return item;
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

    // BQ-DES-005: a void method without @ResponseStatus is documented as 200 with no body.
    @PostMapping("/orders/{id}/archive")
    void archive(@PathVariable String id) {
        // Nothing to archive in a demo.
    }

    // BQ-DES-007: 204 promises no body, but the method returns one.
    @DeleteMapping("/orders/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    Order cancel(@PathVariable String id) {
        return ORDERS.get(0);
    }

    // BQ-DES-008: a partial update sent as plain application/json.
    @PatchMapping("/orders/{id}")
    Order change(@PathVariable String id, @RequestBody Order changes) {
        return changes;
    }

    /**
     * Search criteria sent in the body of a GET. BQ-DES-009: the pattern is not anchored, so JSON Schema
     * validators accept any value that merely contains a capital letter.
     *
     * @param status the status to look for
     */
    record OrderQuery(@Pattern(regexp = "[A-Z]+") String status) {}

    /**
     * A home-grown error body instead of problem details. BQ-NAM-005: one property is snake_case, the others are
     * camelCase.
     *
     * @param errorMessage what went wrong
     * @param errorCode a code for the failure
     * @param requestId the request that failed
     */
    record LegacyError(String errorMessage, @JsonProperty("error_code") String errorCode, String requestId) {}
}
