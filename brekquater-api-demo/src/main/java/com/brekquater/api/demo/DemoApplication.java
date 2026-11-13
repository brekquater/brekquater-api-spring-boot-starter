// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample application for the Brekquater API starter. The {@code good} profile serves an orders API that passes
 * every rule; the {@code bad} profile serves one that breaks each rule at least once.
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] arguments) {
        SpringApplication.run(DemoApplication.class, arguments);
    }
}
