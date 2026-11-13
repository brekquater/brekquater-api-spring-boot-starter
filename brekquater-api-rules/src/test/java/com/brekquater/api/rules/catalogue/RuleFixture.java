// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import com.brekquater.api.rules.Rule;
import com.brekquater.api.rules.RuleResult;
import com.brekquater.api.rules.openapi.OpenApiFixture;
import java.util.List;

/** What a rule reports for an inline YAML fixture, in the form the rule tests assert on. */
record RuleFixture(int applicableTargets, List<String> failingLocations, List<String> messages) {

    static RuleFixture evaluate(Rule rule, String yaml) {
        RuleResult result = rule.evaluate(OpenApiFixture.context(yaml));
        return new RuleFixture(
                result.applicableTargets(),
                result.findings().stream().map(finding -> finding.evidence().location()).toList(),
                result.findings().stream().map(finding -> finding.message()).toList());
    }
}
