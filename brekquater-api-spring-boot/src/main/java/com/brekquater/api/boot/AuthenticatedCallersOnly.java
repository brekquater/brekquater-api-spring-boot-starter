// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Admits callers that Spring Security has authenticated, and rejects anonymous ones, for the {@code authenticated}
 * panel mode. It only reads the security context that the application's own filter chain has set up.
 *
 * <p>This class refers to Spring Security, so it is only loaded after the auto-configuration has checked that
 * Spring Security is on the classpath.
 */
final class AuthenticatedCallersOnly implements AccessGuard {

    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    @Override
    public boolean admitsCurrentCaller() {
        return trustResolver.isAuthenticated(SecurityContextHolder.getContext().getAuthentication());
    }
}
