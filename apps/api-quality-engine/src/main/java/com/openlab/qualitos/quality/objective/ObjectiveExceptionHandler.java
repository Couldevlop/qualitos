package com.openlab.qualitos.quality.objective;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module des objectifs qualité, dits par le module lui-même.
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)} : le socle garde un filet
 * {@code @ExceptionHandler(Exception.class)} qui accepte tout ; sans cette
 * priorité, il répondrait 500 à la place de ce module.
 *
 * <p>La portée reste globale, sans {@code assignableTypes} : un refus de ce
 * module peut être levé pendant une requête adressée à un autre — une
 * vérification d'efficacité de CAPA, par exemple — et il doit garder son statut.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ObjectiveExceptionHandler {

    @ExceptionHandler(ObjectiveNotFoundException.class)
    public ProblemDetail handleObjectiveNotFound(ObjectiveNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "quality-objective-not-found", "Quality Objective Not Found");
    }

    @ExceptionHandler(ProposalNotFoundException.class)
    public ProblemDetail handleProposalNotFound(ProposalNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "quality-objective-proposal-not-found", "Quality Objective Proposal Not Found");
    }

    @ExceptionHandler(ObjectiveValidationException.class)
    public ProblemDetail handleValidation(ObjectiveValidationException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "quality-objective-invalid", "Invalid Quality Objective");
    }

    @ExceptionHandler(ObjectiveStateException.class)
    public ProblemDetail handleState(ObjectiveStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "quality-objective-invalid-state", "Invalid Quality Objective State");
    }
}
