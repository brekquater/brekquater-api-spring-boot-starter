// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

/** Who may see the assessment endpoint and the Swagger UI panel. */
enum PanelMode {
    /** Not served at all: the paths return 404 and nothing is added to Swagger UI. */
    DISABLED,
    /** Served only to callers that Spring Security has authenticated. Requires Spring Security. */
    AUTHENTICATED,
    /** Served to anyone who can reach the application. */
    PUBLIC
}
