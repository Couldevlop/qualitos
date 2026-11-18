package com.openlab.qualitos.quality.costofquality.web;

import com.openlab.qualitos.quality.common.ProblemDetails;
import com.openlab.qualitos.quality.costofquality.domain.CoqNotFoundException;
import com.openlab.qualitos.quality.costofquality.domain.CoqValidationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Les refus du module costofquality, dits par le module lui-meme.
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
public class CostOfQualityExceptionHandler {

    @ExceptionHandler(CoqNotFoundException.class)
    public ProblemDetail handleCoqNotFound(CoqNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "coq-not-found", "Cost Of Quality Item Not Found");
    }


    /**
     * 422 : la ligne est bien formée mais incomplète au regard de son libellé
     * (un contrôle de pièces sans lot, par exemple). {@code field} dit lequel,
     * pour que l'écran l'accroche au bon contrôle.
     */
    @ExceptionHandler(CoqValidationException.class)
    public ProblemDetail handleCoqValidation(CoqValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/coq-invalid"));
        problem.setTitle("Invalid Cost Of Quality Line");
        problem.setProperty("field", ex.getField());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
