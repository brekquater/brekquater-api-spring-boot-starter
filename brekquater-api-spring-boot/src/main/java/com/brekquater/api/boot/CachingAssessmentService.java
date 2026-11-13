// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.report.Assessment;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Keeps the last assessment and reuses it while the document is unchanged (Decorator pattern). The rules
 * therefore run once per version of the document, not once per page load. Two requests that miss the cache at the
 * same moment may both assess; the results are identical, so the race costs time, not correctness.
 */
final class CachingAssessmentService implements AssessmentService {

    // Absent documents share one key, so "no document" is cached like any other state.
    private static final String NO_DOCUMENT = "";

    private final AssessmentService delegate;
    private final AtomicReference<Optional<CachedAssessment>> cache = new AtomicReference<>(Optional.empty());

    CachingAssessmentService(AssessmentService delegate) {
        this.delegate = delegate;
    }

    @Override
    public Assessment assess(Optional<OpenApiDocument> document) {
        String fingerprint = document.map(OpenApiDocument::fingerprint).orElse(NO_DOCUMENT);
        return cache.get()
                .filter(cached -> cached.fingerprint().equals(fingerprint))
                .map(CachedAssessment::assessment)
                .orElseGet(() -> assessAndRemember(document, fingerprint));
    }

    private Assessment assessAndRemember(Optional<OpenApiDocument> document, String fingerprint) {
        Assessment assessment = delegate.assess(document);
        cache.set(Optional.of(new CachedAssessment(fingerprint, assessment)));
        return assessment;
    }

    private record CachedAssessment(String fingerprint, Assessment assessment) {}
}
