// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Explains a startup failure caused by an invalid {@code brekquater.api.*} property. */
final class BrekquaterConfigurationFailureAnalyzer
        extends AbstractFailureAnalyzer<InvalidBrekquaterConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, InvalidBrekquaterConfigurationException cause) {
        String description = "The Brekquater API starter cannot use the value '" + cause.value()
                + "' of the property '" + cause.property() + "'.";
        return new FailureAnalysis(description, cause.fix(), cause);
    }
}
