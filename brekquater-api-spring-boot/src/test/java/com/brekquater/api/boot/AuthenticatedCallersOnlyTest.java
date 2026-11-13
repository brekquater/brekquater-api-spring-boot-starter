// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthenticatedCallersOnlyTest {

    private final AccessGuard guard = new AuthenticatedCallersOnly();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void admitsAnAuthenticatedCaller() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("ada", "n/a", "ROLE_USER"));

        assertThat(guard.admitsCurrentCaller()).isTrue();
    }

    @Test
    void rejectsACallerWithNoAuthentication() {
        assertThat(guard.admitsCurrentCaller()).isFalse();
    }

    @Test
    void rejectsAnAnonymousCaller() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThat(guard.admitsCurrentCaller()).isFalse();
    }

    @Test
    void rejectsAnAuthenticationThatIsNotAuthenticated() {
        TestingAuthenticationToken unverified = new TestingAuthenticationToken("ada", "n/a");
        unverified.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(unverified);

        assertThat(guard.admitsCurrentCaller()).isFalse();
    }
}
