package com.openlab.qualitos.quality.dmaic.resolution.web;

import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionNotFoundException;
import com.openlab.qualitos.quality.dmaic.resolution.domain.ResolutionValidationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Les refus de la fiche de résolution, en RFC 7807. Limité à ses deux
 * contrôleurs : le gestionnaire global n'a pas à la connaître.
 *
 * <p>404 : le projet ou la photo n'est pas chez CE client. 422 : la saisie est
 * bien formée mais refusée par la règle ; {@code field} dit laquelle.
 */
@RestControllerAdvice(assignableTypes = {ResolutionController.class, ResolutionSettingsController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ResolutionExceptionHandler {

    @ExceptionHandler(ResolutionNotFoundException.class)
    public ProblemDetail handleNotFound(ResolutionNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), "dmaic-resolution-not-found",
                "DMAIC Resolution Not Found", null);
    }

    @ExceptionHandler(ResolutionValidationException.class)
    public ProblemDetail handleValidation(ResolutionValidationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "dmaic-resolution-invalid",
                "Invalid DMAIC Resolution Entry", ex.getField());
    }

    private static ProblemDetail problem(HttpStatus status, String detail, String type, String title, String field) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://qualitos.io/errors/" + type));
        problem.setTitle(title);
        if (field != null) {
            problem.setProperty("field", field);
        }
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
