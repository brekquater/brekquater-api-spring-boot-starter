// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo.good;

/** No order has the requested number. */
class OrderNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    OrderNotFoundException(String id) {
        super("Order " + id + " does not exist.");
    }
}
