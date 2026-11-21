// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.rules.catalogue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Recognises URLs that reach another machine over plain {@code http}. Loopback hosts are exempt, because local
 * development servers and identity providers rarely have certificates.
 */
final class PlainHttpUrl {

    private static final String HTTP = "http";
    private static final Set<String> LOOPBACK = Set.of("localhost", "127.0.0.1", "[::1]", "::1");
    private static final String LOOPBACK_DOMAIN = ".localhost";

    private PlainHttpUrl() {}

    /** Returns whether the URL uses {@code http} to reach a host that is not this machine. */
    static boolean reachesAnotherHost(String url) {
        Optional<URI> parsed = parse(url);
        if (parsed.isEmpty() || !HTTP.equalsIgnoreCase(parsed.get().getScheme())) {
            return false;
        }
        String host = Optional.ofNullable(parsed.get().getHost()).orElse("").toLowerCase(Locale.ROOT);
        return !LOOPBACK.contains(host) && !host.endsWith(LOOPBACK_DOMAIN);
    }

    // A URL with server variables, such as {scheme}://api.example.com, is not a URI yet and is not judged.
    private static Optional<URI> parse(String url) {
        try {
            return Optional.of(new URI(url));
        } catch (URISyntaxException exception) {
            return Optional.empty();
        }
    }
}
