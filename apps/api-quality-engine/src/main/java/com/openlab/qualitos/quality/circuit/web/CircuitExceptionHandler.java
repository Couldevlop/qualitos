package com.openlab.qualitos.quality.circuit.web;

import com.openlab.qualitos.quality.common.ProblemDetails;
import com.openlab.qualitos.quality.circuit.domain.CircuitException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Les refus du module circuit, dits par le module lui-meme.
 *
 * <p>Ces gestionnaires vivaient dans {@code common/GlobalExceptionHandler}, qui
 * devait importer l exception de chaque module pour savoir quel statut lui
 * repondre. Le socle dependait ainsi de soixante-six modules, et tout module
 * neuf devait MODIFIER un fichier partage pour que ses refus sortent avec le bon
 * statut (ADR 0128).
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
public class CircuitExceptionHandler {


    /**
     * Circuits de validation (ADR 0080) : 422 circuit mal formé, 404 type d'objet
     * inconnu, 403 étape qui revient à un autre rôle, 409 état qui ne permet pas
     * la décision (auteur, déjà décidé, circuit terminé).
     */
    @ExceptionHandler(com.openlab.qualitos.quality.circuit.domain.CircuitException.class)
    public ProblemDetail handleCircuit(com.openlab.qualitos.quality.circuit.domain.CircuitException ex) {
        HttpStatus status = switch (ex.getReason()) {
            case INVALID -> HttpStatus.UNPROCESSABLE_ENTITY;
            case UNKNOWN_SUBJECT -> HttpStatus.NOT_FOUND;
            case NOT_YOUR_STEP -> HttpStatus.FORBIDDEN;
            case NOT_IN_PROGRESS, AUTHOR_CANNOT_DECIDE, ALREADY_DECIDED -> HttpStatus.CONFLICT;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/circuit-"
                + ex.getReason().name().toLowerCase(java.util.Locale.ROOT).replace('_', '-')));
        problem.setTitle("Approval Circuit");
        problem.setProperty("reason", ex.getReason().name());
        problem.setProperty("field", ex.getField());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
