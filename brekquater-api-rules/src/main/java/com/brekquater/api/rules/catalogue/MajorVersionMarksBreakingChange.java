// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.ApiChanges;
import com.brekquater.api.rules.AssessmentContext;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Evidence;
import com.brekquater.api.rules.Explanation;
import com.brekquater.api.rules.Finding;
import com.brekquater.api.rules.Rule;
import com.brekquater.api.rules.RuleInput;
import com.brekquater.api.rules.RuleResult;
import com.brekquater.api.rules.Severity;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BQ-EVO-008: when the document has a breaking change since the baseline, the major version in {@code info.version}
 * went up, or a dated version moved to a later date, so clients can tell from the version alone. The breaking
 * changes themselves are reported by the other BQ-EVO rules, which is why this one is MINOR.
 */
public final class MajorVersionMarksBreakingChange implements Rule {

    private static final Pattern MAJOR = Pattern.compile("v?(\\d+).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATED = Pattern.compile("\\d{4}-\\d{2}-\\d{2}(-preview)?");
    private static final String INFO_VERSION = "/info/version";

    @Override
    public String id() {
        return "BQ-EVO-008";
    }

    @Override
    public Severity severity() {
        return Severity.MINOR;
    }

    @Override
    public Category category() {
        return Category.DESIGN;
    }

    @Override
    public RuleInput input() {
        return RuleInput.BASELINE;
    }

    @Override
    public RuleResult evaluate(AssessmentContext context) {
        return context.changes().filter(ApiChanges::breaking).map(this::evaluate).orElse(RuleResult.notApplicable());
    }

    private RuleResult evaluate(ApiChanges changes) {
        Optional<String> before = changes.baselineVersion().map(String::strip);
        Optional<String> after = changes.currentVersion().map(String::strip);
        if (before.isEmpty() || after.isEmpty()) {
            return RuleResult.notApplicable();
        }
        return newRelease(before.get(), after.get())
                .map(released -> released ? RuleResult.of(1, List.of()) : failure(before.get(), after.get()))
                .orElse(RuleResult.notApplicable());
    }

    // A dated version (2026-11-27) names a whole release, so any later date marks a breaking change; mixing dated
    // and numbered versions cannot be compared.
    private static Optional<Boolean> newRelease(String before, String after) {
        boolean beforeDated = DATED.matcher(before).matches();
        if (beforeDated != DATED.matcher(after).matches()) {
            return Optional.empty();
        }
        if (beforeDated) {
            return Optional.of(after.compareTo(before) > 0);
        }
        OptionalInt oldMajor = major(before);
        OptionalInt newMajor = major(after);
        if (oldMajor.isEmpty() || newMajor.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(newMajor.getAsInt() > oldMajor.getAsInt());
    }

    private RuleResult failure(String before, String after) {
        Explanation explanation = new Explanation(
                "The API has breaking changes since " + before + ", but its version " + after + " does not mark a "
                        + "new major release.",
                "Raise the major version, for example from 1.4.0 to 2.0.0, or for a dated version publish the change "
                        + "under a later date; or undo the breaking changes the other BQ-EVO rules report.");
        return RuleResult.of(1, List.of(Finding.of(this, new Evidence(RuleInput.BASELINE, INFO_VERSION), explanation)));
    }

    private static OptionalInt major(String version) {
        Matcher matcher = MAJOR.matcher(version);
        return matcher.matches() ? OptionalInt.of(Integer.parseInt(matcher.group(1))) : OptionalInt.empty();
    }
}
