package com.openlab.qualitos.quality.riskregister.web;

import com.openlab.qualitos.quality.common.ProblemDetails;
import com.openlab.qualitos.quality.riskregister.domain.RegisterNotFoundException;
import com.openlab.qualitos.quality.riskregister.domain.RegisterValidationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Les refus du module riskregister, dits par le module lui-meme.
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
public class RiskRegisterExceptionHandler {

    @ExceptionHandler(RegisterNotFoundException.class)
    public ProblemDetail handleRegisterNotFound(RegisterNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "risk-register-not-found", "Risk Register Item Not Found");
    }


    /**
     * 422 : la fiche est bien formée mais incohérente (résiduelle au-dessus de
     * la brute, exigence d'un autre registre…). {@code field} dit laquelle.
     */
    @ExceptionHandler(RegisterValidationException.class)
    public ProblemDetail handleRegisterValidation(RegisterValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/risk-register-invalid"));
        problem.setTitle("Invalid Risk Register Item");
        problem.setProperty("field", ex.getField());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
