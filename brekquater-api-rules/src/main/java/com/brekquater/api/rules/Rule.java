// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

/**
 * One API quality check (Strategy pattern). A rule checks exactly one thing and is stateless.
 *
 * <p>Built-in rules are discovered with {@link java.util.ServiceLoader}, so an implementation needs a public
 * no-argument constructor and an entry in {@code META-INF/services/com.brekquater.api.rules.Rule}.
 *
 * <p>Rule IDs are permanent: an ID is never reused, and a breaking change in behaviour ships under a new ID.
 */
public interface Rule {

    /** Returns the stable ID, for example {@code BQ-DOC-001}. */
    String id();

    Severity severity();

    Category category();

    /** Returns the artefact this rule reads. The engine does not call {@link #evaluate} when it is absent. */
    RuleInput input();

    /**
     * Checks every target this rule applies to.
     *
     * @param context the available inputs; the one named by {@link #input()} is present
     * @return the number of targets checked and one finding per failing target
     */
    RuleResult evaluate(AssessmentContext context);
}
