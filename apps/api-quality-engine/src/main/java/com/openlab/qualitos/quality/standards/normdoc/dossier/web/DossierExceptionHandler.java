package com.openlab.qualitos.quality.standards.normdoc.dossier.web;

import com.openlab.qualitos.quality.standards.StandardNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierNotFoundException;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierStateException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Mapping RFC 7807 des erreurs du module de génération documentaire
 * multi-documents (§8.8). Scopé à {@link DossierController} (clean architecture,
 * pas de modification du handler global). Priorité haute pour primer sur le
 * fallback {@code Exception → 500}.
 */
@RestControllerAdvice(assignableTypes = DossierController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DossierExceptionHandler {

    @ExceptionHandler({DossierNotFoundException.class, StandardNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/standard-doc-dossier-not-found"));
        problem.setTitle("Documentation Dossier Not Found");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(DossierStateException.class)
    public ProblemDetail handleState(DossierStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/standard-doc-dossier-invalid-state"));
        problem.setTitle("Invalid Documentation Dossier State");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/standard-doc-dossier-invalid-input"));
        problem.setTitle("Invalid Documentation Dossier Input");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
