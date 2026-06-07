package com.openlab.qualitos.quality.workflow;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Advice local au module Workflow (§5.4).
 *
 * <p>Le {@code GlobalExceptionHandler} partagé déclare un catch-all
 * {@code @ExceptionHandler(Exception.class)} → 500. Comme Spring résout les
 * handlers par <em>spécificité du type d'exception</em> (toutes advices
 * confondues), un handler ciblant nos exceptions concrètes ici prime sur le
 * catch-all : on obtient bien 404 / 409 / 400 sans toucher au handler global
 * (interdit par la consigne). C'est aussi pourquoi le {@code @ResponseStatus}
 * porté par les exceptions ne suffit pas — il est court-circuité par le
 * catch-all.</p>
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)} : Spring consulte les advices dans
 * l'ordre et retient le PREMIER qui possède un handler applicable. Le
 * {@code GlobalExceptionHandler} (non ordonné) a un handler {@code Exception}
 * qui matche tout ; sans priorité, il intercepterait nos exceptions en 500.
 * On passe donc devant lui.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = WorkflowController.class)
public class WorkflowExceptionHandler {

    @ExceptionHandler(WorkflowNotFoundException.class)
    public ProblemDetail handleNotFound(WorkflowNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(),
                "workflow-definition-not-found", "Workflow Definition Not Found");
    }

    @ExceptionHandler(WorkflowStateException.class)
    public ProblemDetail handleState(WorkflowStateException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(),
                "workflow-invalid-state", "Invalid Workflow State");
    }

    @ExceptionHandler(WorkflowValidationException.class)
    public ProblemDetail handleValidation(WorkflowValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "workflow-invalid", "Invalid Workflow Definition");
    }

    private ProblemDetail problem(HttpStatus status, String detail, String typeSlug, String title) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://qualitos.io/errors/" + typeSlug));
        problem.setTitle(title);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
