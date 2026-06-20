package com.openlab.qualitos.quality.standards.normdoc.web;

import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocStateException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Mapping RFC 7807 des erreurs du module de génération de documents normatifs
 * (§8.8). Scopé à {@link NormDocController} pour rester dans le périmètre
 * {@code standards/**} (clean architecture, pas de modification du handler global).
 *
 * <p>Priorité haute : prime sur le {@code GlobalExceptionHandler} (qui possède un
 * fallback {@code Exception → 500}) afin que les erreurs spécifiques du module
 * soient mappées 404/409/400.
 */
@RestControllerAdvice(assignableTypes = NormDocController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NormDocExceptionHandler {

    @ExceptionHandler(NormDocNotFoundException.class)
    public ProblemDetail handleNotFound(NormDocNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/standard-norm-document-not-found"));
        problem.setTitle("Normative Document Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NormDocStateException.class)
    public ProblemDetail handleState(NormDocStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/standard-norm-document-invalid-state"));
        problem.setTitle("Invalid Normative Document State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/standard-norm-document-invalid-input"));
        problem.setTitle("Invalid Normative Document Input");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
