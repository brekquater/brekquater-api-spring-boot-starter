// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import java.util.Objects;

/**
 * A test that failed or ended with an error.
 *
 * @param className the test class, as the test report names it
 * @param name the test method or display name
 */
public record FailedTest(String className, String name) {

    public FailedTest {
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(name, "name");
    }

    /** Returns the test as {@code Class#method}, the form developers search for. */
    public String location() {
        return className + "#" + name;
    }
}
