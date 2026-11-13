// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * The application's OpenAPI document as JSON text, with a fingerprint that changes whenever the text does.
 *
 * @param json the document as springdoc serves it
 * @param fingerprint the SHA-256 of the text, used to tell whether a cached assessment is still current
 */
public record OpenApiDocument(String json, String fingerprint) {

    private static final String FINGERPRINT_ALGORITHM = "SHA-256";

    public OpenApiDocument {
        Objects.requireNonNull(json, "json");
        Objects.requireNonNull(fingerprint, "fingerprint");
    }

    public static OpenApiDocument of(String json) {
        return new OpenApiDocument(json, sha256(json));
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance(FINGERPRINT_ALGORITHM).digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            // Every Java platform is required to provide SHA-256, so this cannot happen on a conforming JVM.
            throw new IllegalStateException(FINGERPRINT_ALGORITHM + " is not available", exception);
        }
    }
}
