// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.scoring;

/** Letter shown beside the overall score, declared from best to worst. */
public enum Grade {
    A,
    B,
    C,
    D,
    /** The lowest grade. It has no minimum score: any score below the minimum for {@link #D} gets it. */
    F
}
