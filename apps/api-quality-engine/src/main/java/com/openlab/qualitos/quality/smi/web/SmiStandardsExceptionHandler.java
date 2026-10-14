package com.openlab.qualitos.quality.smi.web;

import com.openlab.qualitos.quality.smi.domain.StandardUnavailableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Les refus de l'ajout d'une norme au SMI, en RFC 7807. Limité à
 * {@link SmiStandardsController} : le gestionnaire global n'a pas à connaître
 * le SMI. Une norme déjà adoptée se dit 409, comme dans le Standards Hub ; une
 * norme qui n'est pas de système de management, 404 — pour le SMI, elle
 * n'existe pas.
 */
@RestControllerAdvice(assignableTypes = SmiStandardsController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SmiStandardsExceptionHandler {

    @ExceptionHandler(StandardUnavailableException.class)
    public ProblemDetail handleUnavailable(StandardUnavailableException ex) {
        boolean dejaAdoptee = ex.reason() == StandardUnavailableException.Reason.ALREADY_ADOPTED;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                dejaAdoptee ? HttpStatus.CONFLICT : HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create(dejaAdoptee
                ? "https://qualitos.io/errors/smi-standard-already-adopted"
                : "https://qualitos.io/errors/smi-standard-not-management-system"));
        problem.setTitle(dejaAdoptee ? "Standard Already Adopted" : "Not A Management System Standard");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
