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
 * went up, so clients can tell from the version alone. The breaking changes themselves are reported by the other
 * BQ-EVO rules, which is why this one is MINOR.
 */
public final class MajorVersionMarksBreakingChange implements Rule {

    private static final Pattern MAJOR = Pattern.compile("v?(\\d+).*", Pattern.CASE_INSENSITIVE);
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
        OptionalInt before = major(changes.baselineVersion());
        OptionalInt after = major(changes.currentVersion());
        if (before.isEmpty() || after.isEmpty()) {
            return RuleResult.notApplicable();
        }
        if (after.getAsInt() > before.getAsInt()) {
            return RuleResult.of(1, List.of());
        }
        Explanation explanation = new Explanation(
                "The API has breaking changes since " + changes.baselineVersion().orElseThrow() + ", but its version "
                        + changes.currentVersion().orElseThrow() + " has the same major version.",
                "Raise the major version, for example to " + (before.getAsInt() + 1) + ".0.0, or undo the breaking "
                        + "changes the other BQ-EVO rules report.");
        return RuleResult.of(1, List.of(Finding.of(this, new Evidence(RuleInput.BASELINE, INFO_VERSION), explanation)));
    }

    private static OptionalInt major(Optional<String> version) {
        if (version.isEmpty()) {
            return OptionalInt.empty();
        }
        Matcher matcher = MAJOR.matcher(version.get().strip());
        return matcher.matches() ? OptionalInt.of(Integer.parseInt(matcher.group(1))) : OptionalInt.empty();
    }
}
