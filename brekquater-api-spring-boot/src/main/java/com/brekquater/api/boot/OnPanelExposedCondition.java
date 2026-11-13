// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/** Matches when the assessment may be shown to someone, so a disabled panel registers no handler at all. */
final class OnPanelExposedCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        PanelMode mode = PanelExposure.modeIn(context.getEnvironment());
        ConditionMessage.Builder message = ConditionMessage.forCondition("Brekquater API panel exposure");
        return mode == PanelMode.DISABLED
                ? ConditionOutcome.noMatch(message.because("the panel mode is disabled"))
                : ConditionOutcome.match(message.because("the panel mode is " + mode.name().toLowerCase(Locale.ROOT)));
    }
}
