// SPDX-License-Identifier: Apache-2.0
// Copyright Brekquater

package com.brekquater.api.boot;

import com.brekquater.api.rules.report.Assessment;
import com.brekquater.api.rules.report.ReportWriter;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import java.io.StringWriter;
import java.util.function.Supplier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the assessment as the versioned JSON contract, and the document it assessed. Both are read-only, hidden from
 * the application's own OpenAPI document, and never return a stack trace or exception message.
 */
@Hidden
@RestController
@RequestMapping("${brekquater.api.panel.base-path:/brekquater/api}")
final class AssessmentEndpoint {

    private static final Log LOGGER = LogFactory.getLog(AssessmentEndpoint.class);

    private final AccessGuard guard;
    private final OpenApiDocumentSource source;
    private final AssessmentService service;
    private final ReportWriter writer;

    AssessmentEndpoint(Collaborators collaborators) {
        this.guard = collaborators.guard();
        this.source = collaborators.source();
        this.service = collaborators.service();
        this.writer = collaborators.writer();
    }

    @GetMapping(path = "/assessment", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> assessment(HttpServletRequest request) {
        return guarded(() -> ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_JSON)
                .body(json(service.assess(source.fetch(request)))));
    }

    /**
     * The document that was assessed, so the dashboard page can list the operations. Served here rather than read
     * from springdoc's own path so it follows the same guard, and works with any {@link OpenApiDocumentSource}.
     */
    @GetMapping(path = "/openapi", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> document(HttpServletRequest request) {
        return guarded(() -> source.fetch(request)
                .map(document -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(document.json()))
                .orElseGet(() -> problem(HttpStatus.NOT_FOUND)));
    }

    private ResponseEntity<String> guarded(Supplier<ResponseEntity<String>> response) {
        if (!guard.admitsCurrentCaller()) {
            return problem(HttpStatus.UNAUTHORIZED);
        }
        try {
            return response.get();
        } catch (RuntimeException exception) {
            LOGGER.error("The Brekquater endpoint failed with " + exception.getClass().getName(), exception);
            return problem(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String json(Assessment assessment) {
        StringWriter output = new StringWriter();
        writer.write(assessment, output);
        return output.toString();
    }

    // A minimal RFC 9457 body: the status says enough, and anything more could reveal internals.
    private static ResponseEntity<String> problem(HttpStatus status) {
        String body = "{\"title\":\"" + status.getReasonPhrase() + "\",\"status\":" + status.value() + "}";
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    /** What the endpoint needs, grouped so its constructor stays small. */
    record Collaborators(
            AccessGuard guard, OpenApiDocumentSource source, AssessmentService service, ReportWriter writer) {}
}
