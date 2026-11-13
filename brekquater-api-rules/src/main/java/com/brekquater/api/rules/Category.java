// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

/**
 * Quality area a rule belongs to. A category with no evaluated rule is reported as "not assessed" and never counts
 * as full marks.
 */
public enum Category {
    DESIGN,
    DOCUMENTATION,
    SECURITY,
    ERRORS,
    NAMING,
    TESTING,
    CODE_QUALITY,
    ARCHITECTURE
}
