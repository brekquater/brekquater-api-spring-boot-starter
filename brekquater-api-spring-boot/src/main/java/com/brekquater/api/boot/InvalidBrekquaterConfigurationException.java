// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

/**
 * A {@code brekquater.api.*} property has a value the starter cannot use. {@link
 * BrekquaterConfigurationFailureAnalyzer} turns it into a startup report that names the property, the value and
 * the fix.
 */
final class InvalidBrekquaterConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String property;
    private final String value;
    private final String fix;

    InvalidBrekquaterConfigurationException(String property, Object value, String fix) {
        super("Invalid value '" + value + "' for property '" + property + "'. " + fix);
        this.property = property;
        this.value = String.valueOf(value);
        this.fix = fix;
    }

    String property() {
        return property;
    }

    String value() {
        return value;
    }

    String fix() {
        return fix;
    }
}
