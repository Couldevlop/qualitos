package com.openlab.qualitos.quality.circle;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module circle, dits par le module lui-meme.
 *
 * <p>Ces gestionnaires vivaient dans {@code common/GlobalExceptionHandler}, qui
 * devait importer l exception de chaque module pour savoir quel statut lui
 * repondre. Le socle dependait ainsi de soixante-six modules, et tout module
 * neuf devait MODIFIER un fichier partage pour que ses refus sortent avec le bon
 * statut.
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)} : Spring consulte les conseils dans
 * l ordre et retient le PREMIER qui possede un gestionnaire applicable. Le socle
 * garde un filet {@code @ExceptionHandler(Exception.class)} qui accepte tout ;
 * sans cette priorite, il repondrait 500 a la place de ce module.
 *
 * <p>La portee reste GLOBALE, sans {@code assignableTypes} : une exception de ce
 * module peut etre levee pendant une requete adressee a un autre module -- un
 * ecart qui ouvre un dossier CAPA, par exemple -- et elle doit garder son statut.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class CircleExceptionHandler {

    @ExceptionHandler(CircleNotFoundException.class)
    public ProblemDetail handleCircleNotFound(CircleNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "circle-not-found", "Quality Circle Not Found");
    }

    @ExceptionHandler(CircleMemberNotFoundException.class)
    public ProblemDetail handleCircleMemberNotFound(CircleMemberNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "circle-member-not-found", "Circle Member Not Found");
    }

    @ExceptionHandler(CircleMeetingNotFoundException.class)
    public ProblemDetail handleCircleMeetingNotFound(CircleMeetingNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "circle-meeting-not-found", "Circle Meeting Not Found");
    }

    @ExceptionHandler(CircleProposalNotFoundException.class)
    public ProblemDetail handleCircleProposalNotFound(CircleProposalNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "circle-proposal-not-found", "Circle Proposal Not Found");
    }

    @ExceptionHandler(CircleStateException.class)
    public ProblemDetail handleCircleState(CircleStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "circle-invalid-state", "Invalid Quality Circle State");
    }
}
