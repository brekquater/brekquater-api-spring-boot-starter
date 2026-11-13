// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;

import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.report.Assessment;
import com.brekquater.api.rules.scoring.Score;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CachingAssessmentServiceTest {

    private static final Optional<OpenApiDocument> ORDERS = Optional.of(OpenApiDocument.of("{\"openapi\":\"3.1.0\"}"));
    private static final Optional<OpenApiDocument> CHANGED = Optional.of(OpenApiDocument.of("{\"openapi\":\"3.0.3\"}"));

    private final AtomicInteger assessments = new AtomicInteger();
    private final CachingAssessmentService service = new CachingAssessmentService(document -> {
        assessments.incrementAndGet();
        return assessment();
    });

    @Test
    void assessesAnUnchangedDocumentOnlyOnce() {
        Assessment first = service.assess(ORDERS);
        Assessment second = service.assess(ORDERS);

        assertThat(second).isSameAs(first);
        assertThat(assessments).hasValue(1);
    }

    @Test
    void assessesAgainWhenTheDocumentChanges() {
        service.assess(ORDERS);
        service.assess(CHANGED);
        service.assess(CHANGED);

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
        service.assess(ORDERS);

        assertThat(assessments).hasValue(2);
    }

    private static Assessment assessment() {
        AssessmentResult result =
                new AssessmentResult("test", Optional.empty(), List.of(), List.of(), List.of(), List.of());
        return new Assessment(result, new Score(OptionalInt.empty(), Optional.empty(), List.of()), Instant.EPOCH);
    }
}
