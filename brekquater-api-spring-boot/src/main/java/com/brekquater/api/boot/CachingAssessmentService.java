// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.report.Assessment;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Keeps the last assessment and reuses it while its inputs are unchanged (Decorator pattern). The rules therefore
 * run once per version of the document and the build reports, not once per page load. Two requests that miss the
 * cache at the same moment may both assess; the results are identical, so the race costs time, not correctness.
 */
final class CachingAssessmentService implements AssessmentService {

    private final AssessmentService delegate;
    private final Function<Optional<OpenApiDocument>, String> fingerprint;
    private final AtomicReference<Optional<CachedAssessment>> cache = new AtomicReference<>(Optional.empty());

    /**
     * @param delegate makes the assessments
     * @param fingerprint returns a value that differs whenever an input of the assessment differs
     */
    CachingAssessmentService(AssessmentService delegate, Function<Optional<OpenApiDocument>, String> fingerprint) {
        this.delegate = delegate;
        this.fingerprint = fingerprint;
    }

    @Override
    public Assessment assess(Optional<OpenApiDocument> document) {
        String key = fingerprint.apply(document);
        return cache.get()
                .filter(cached -> cached.key().equals(key))
                .map(CachedAssessment::assessment)
                .orElseGet(() -> remember(key, delegate.assess(document)));
    }

    private Assessment remember(String key, Assessment assessment) {
        cache.set(Optional.of(new CachedAssessment(key, assessment)));
        return assessment;
    }

    private record CachedAssessment(String key, Assessment assessment) {}
}
