// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

class JsonPointerPropertyTest {

    @Property
    void anyTokensSurviveEscapingAndUnescaping(@ForAll List<String> tokens) {
        JsonPointer pointer = tokens.stream().reduce(JsonPointer.root(), JsonPointer::append, (first, second) -> first);

        assertThat(pointer.tokens()).isEqualTo(tokens);
    }

    @Property
    void aPointerBuiltFromTokensIsAlwaysAValidPointer(@ForAll List<String> tokens) {
        JsonPointer pointer = tokens.stream().reduce(JsonPointer.root(), JsonPointer::append, (first, second) -> first);

        assertThat(new JsonPointer(pointer.value())).isEqualTo(pointer);
    }
}
