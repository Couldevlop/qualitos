package com.openlab.qualitos.quality.spc;

import com.openlab.qualitos.quality.common.ProblemDetails;
import com.openlab.qualitos.quality.spc.capability.domain.SpcNotFoundException;
import com.openlab.qualitos.quality.spc.capability.domain.SpcValidationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Les refus du module spc, dits par le module lui-meme.
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
public class SpcExceptionHandler {

    @ExceptionHandler(SpcNotFoundException.class)
    public ProblemDetail handleSpcNotFound(SpcNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "spc-study-not-found", "SPC Study Not Found");
    }


    /**
     * 422 : l'étude est bien formée mais incohérente (USL sous LSL, aucun
     * sous-groupe complet, seuils inversés). {@code field} dit lequel, pour que
     * l'écran l'accroche au bon contrôle.
     */
    @ExceptionHandler(SpcValidationException.class)
    public ProblemDetail handleSpcValidation(SpcValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/spc-study-invalid"));
        problem.setTitle("Invalid SPC Study");
        problem.setProperty("field", ex.getField());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
