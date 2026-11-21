// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo.good;

import com.brekquater.api.demo.Order;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** An orders API that follows every rule. */
@Profile("good")
@RestController
@RequestMapping("/orders")
class OrdersController {

    private static final Map<String, Order> ORDERS =
            Map.of("1001", new Order("1001", "SHIPPED"), "1002", new Order("1002", "PENDING"));
    private static final String NEW_ORDER_ID = "1003";
    private static final String NEW_ORDER_STATUS = "PENDING";
    private static final String MERGE_PATCH = "application/merge-patch+json";

    @Operation(summary = "List orders, one page at a time")
    @GetMapping
    List<Order> listOrders(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ORDERS.values().stream().sorted((a, b) -> a.id().compareTo(b.id())).skip((long) page * size).limit(size).toList();
    }

    @Operation(summary = "Find one order by its number")
    @GetMapping("/{id}")
    Order findOrder(@PathVariable String id) {
        return Optional.ofNullable(ORDERS.get(id)).orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Operation(summary = "Place an order")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Order placeOrder(@RequestBody NewOrder order) {
        return new Order(NEW_ORDER_ID, NEW_ORDER_STATUS);
    }

    @Operation(summary = "Cancel an order")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelOrder(@PathVariable String id) {
        findOrder(id);
    }

    // JSON Merge Patch (RFC 7396): fields that are sent are replaced, fields that are left out stay as they are.
    @Operation(summary = "Change the status of an order")
    @PatchMapping(path = "/{id}", consumes = MERGE_PATCH)
    Order changeOrder(@PathVariable String id, @RequestBody OrderChange change) {
        return new Order(findOrder(id).id(), change.status());
    }

    /**
     * The fields of an order a client may change.
     *
     * @param status the new status
     */
    record OrderChange(String status) {}

    /**
     * What a client sends to place an order.
     *
     * @param product the product to order
     * @param quantity how many to order
     */
    record NewOrder(String product, int quantity) {}
}
