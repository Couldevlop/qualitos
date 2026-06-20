package com.openlab.qualitos.quality.standards.auditblanc.web;

import com.openlab.qualitos.quality.standards.TenantStandardNotFoundException;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRunNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Mapping RFC 7807 des erreurs du module d'audit blanc IA (§8.4 onglet 7).
 * Scopé à {@link MockAuditController} pour rester dans le périmètre
 * {@code standards/auditblanc/**} (clean architecture, pas de modification du
 * handler global). Priorité haute : prime sur le {@code GlobalExceptionHandler}.
 */
@RestControllerAdvice(assignableTypes = MockAuditController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MockAuditExceptionHandler {

    @ExceptionHandler({MockAuditRunNotFoundException.class, TenantStandardNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/standard-mock-audit-not-found"));
        problem.setTitle("Mock Audit Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleState(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/standard-mock-audit-invalid-state"));
        problem.setTitle("Invalid Mock Audit State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/standard-mock-audit-invalid-input"));
        problem.setTitle("Invalid Mock Audit Input");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
