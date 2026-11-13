// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

/**
 * Artefact a rule reads. The engine skips a rule whose input is absent instead of calling it, so the report can say
 * why a category was not assessed.
 */
public enum RuleInput {
    /** The application's OpenAPI document. */
    OPENAPI
}
