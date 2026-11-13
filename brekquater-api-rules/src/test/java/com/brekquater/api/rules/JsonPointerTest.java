// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class JsonPointerTest {

    @Test
    void theRootPointerIsEmpty() {
        assertThat(JsonPointer.root().value()).isEmpty();
        assertThat(JsonPointer.root().tokens()).isEmpty();
    }

    @Test
    void escapesTheSlashesOfAPath() {
        JsonPointer pointer = JsonPointer.root().append("paths").append("/orders/{id}").append("get");

        assertThat(pointer.value()).isEqualTo("/paths/~1orders~1{id}/get");
    }

    @Test
    void escapesATildeBeforeASlashSoThatBothSurvive() {
        JsonPointer pointer = JsonPointer.root().append("a~/b");

        assertThat(pointer.value()).isEqualTo("/a~0~1b");
        assertThat(pointer.tokens()).containsExactly("a~/b");
    }

    @Test
    void decodesAnEscapedTildeFollowedByADigitOneAsATildeAndAOne() {
        assertThat(new JsonPointer("/~01").tokens()).containsExactly("~1");
    }

    @Test
    void keepsAnEmptyToken() {
        assertThat(JsonPointer.root().append("").value()).isEqualTo("/");
        assertThat(new JsonPointer("/").tokens()).containsExactly("");
    }

    @Test
    void printsAsItsValue() {
        assertThat(JsonPointer.root().append("info")).hasToString("/info");
    }

    @Test
    void rejectsTextThatDoesNotStartWithASlash() {
        assertThatIllegalArgumentException().isThrownBy(() -> new JsonPointer("paths"));
    }
}
