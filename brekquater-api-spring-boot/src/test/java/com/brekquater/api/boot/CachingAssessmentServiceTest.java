// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.BuildReports;
import com.brekquater.api.rules.report.Assessment;
import com.brekquater.api.rules.scoring.Score;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CachingAssessmentServiceTest {

    private static final OpenApiDocument ORDERS = OpenApiDocument.of("{\"openapi\":\"3.1.0\"}");
    private static final OpenApiDocument CHANGED = OpenApiDocument.of("{\"openapi\":\"3.0.3\"}");

    private final AtomicInteger assessments = new AtomicInteger();
    private final AtomicReference<String> reportsFingerprint = new AtomicReference<>("jacoco.xml:100");
    private final CachingAssessmentService service = new CachingAssessmentService(
            document -> {
                assessments.incrementAndGet();
                return assessment();
            },
            document -> document.map(OpenApiDocument::fingerprint).orElse("") + reportsFingerprint.get());

    @Test
    void assessesAnUnchangedDocumentOnlyOnce() {
        Assessment first = service.assess(Optional.of(ORDERS));
        Assessment second = service.assess(Optional.of(ORDERS));

        assertThat(second).isSameAs(first);
        assertThat(assessments).hasValue(1);
    }

    @Test
    void assessesAgainWhenTheDocumentChanges() {
        service.assess(Optional.of(ORDERS));
        service.assess(Optional.of(CHANGED));
        service.assess(Optional.of(CHANGED));

        assertThat(assessments).hasValue(2);
    }

    @Test
    void remembersThatThereIsNoDocument() {
        service.assess(Optional.empty());
        service.assess(Optional.empty());

        assertThat(assessments).hasValue(1);
    }

    @Test
    void aDocumentAppearingAfterNoneIsAssessed() {
        service.assess(Optional.empty());
        service.assess(Optional.of(ORDERS));

        assertThat(assessments).hasValue(2);
    }

    @Test
    void assessesAgainWhenABuildReportChanges() {
        service.assess(Optional.of(ORDERS));
        reportsFingerprint.set("jacoco.xml:200");
        service.assess(Optional.of(ORDERS));

        assertThat(assessments).hasValue(2);
    }

    private static Assessment assessment() {
        AssessmentResult result =
                new AssessmentResult(
                        "test", Optional.empty(), BuildReports.none(), List.of(), List.of(), List.of(), List.of());
        return new Assessment(result, new Score(OptionalInt.empty(), Optional.empty(), List.of()), Instant.EPOCH);
    }
}
