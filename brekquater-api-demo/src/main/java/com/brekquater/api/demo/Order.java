// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo;

/**
 * An order, as both demo APIs return it.
 *
 * @param id the order number
 * @param status where the order is in its life cycle, for example PENDING or SHIPPED
 */
public record Order(String id, String status) {}
