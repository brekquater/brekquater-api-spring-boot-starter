// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.brekquater.api.rules.AssessmentResult;
import com.brekquater.api.rules.BuildReports;
import com.brekquater.api.rules.Category;
import com.brekquater.api.rules.Evidence;
import com.brekquater.api.rules.Finding;
import com.brekquater.api.rules.RuleInput;
import com.brekquater.api.rules.Severity;
import com.brekquater.api.rules.report.Assessment;
import com.brekquater.api.rules.scoring.Score;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;

class StrictModeCheckTest {

    private final AtomicReference<HttpServletRequest> requestSeen = new AtomicReference<>();
    private final OpenApiDocumentSource source = request -> {
        requestSeen.set(request);
        return Optional.empty();
    };

    @Test
    void stopsStartupWhenABlockerIsFound() {
        StrictModeCheck check = new StrictModeCheck(source, document -> assessmentWith(finding(Severity.BLOCKER)), "");

        assertThatExceptionOfType(StrictModeViolationException.class)
                .isThrownBy(check::afterSingletonsInstantiated)
                .withMessage("Strict mode stopped startup: 1 blocker finding(s) from BQ-SEC-001.");
    }

    @Test
    void letsStartupContinueWhenNoFindingIsABlocker() {
        StrictModeCheck check = new StrictModeCheck(source, document -> assessmentWith(finding(Severity.MAJOR)), "");

        assertThatNoException().isThrownBy(check::afterSingletonsInstantiated);
    }

    @Test
    void readsTheDocumentAsIfRequestedUnderTheContextPath() {
        new StrictModeCheck(source, document -> assessmentWith(), "/shop").afterSingletonsInstantiated();

        assertThat(requestSeen.get().getContextPath()).isEqualTo("/shop");
    }

    @Test
    void explainsWhichBlockersStoppedStartupAndHowToProceed() {
        StrictModeViolationException violation = new StrictModeViolationException(List.of(finding(Severity.BLOCKER)));

        FailureAnalysis analysis = Objects.requireNonNull(new StrictModeFailureAnalyzer().analyze(violation));

        assertThat(analysis.getDescription())
                .contains("found 1 blocker finding(s)", "BQ-SEC-001 at /paths/~1orders/get: Unprotected.");
        assertThat(analysis.getAction()).contains("x-brekquater-ignore", "brekquater.api.strict=false");
    }

    @Test
    void listsAtMostTenBlockers() {
        List<Finding> twelve = IntStream.range(0, 12).mapToObj(index -> finding(Severity.BLOCKER)).toList();

        FailureAnalysis analysis =
                Objects.requireNonNull(new StrictModeFailureAnalyzer().analyze(new StrictModeViolationException(twelve)));

        assertThat(analysis.getDescription()).contains("... and 2 more");
    }

    private static Finding finding(Severity severity) {
        return new Finding("BQ-SEC-001", severity, Category.SECURITY, "Unprotected.",
                new Evidence(RuleInput.OPENAPI, "/paths/~1orders/get"), "Protect it.", "https://x");
    }

    private static Assessment assessmentWith(Finding... findings) {
        AssessmentResult result = new AssessmentResult(
                "test", Optional.empty(), BuildReports.none(), List.of(findings), List.of(), List.of(), List.of());
        return new Assessment(result, new Score(OptionalInt.empty(), Optional.empty(), List.of()), Instant.EPOCH);
    }
}
