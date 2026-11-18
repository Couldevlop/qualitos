package com.openlab.qualitos.quality.fmeascale;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module fmeascale, dits par le module lui-meme.
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
public class FmeaScaleExceptionHandler {


    /**
     * Barème de cotation refusé : trou dans l'échelle, doublon, intitulé vide.
     * Le message NOMME ce qui manque — « le score 7 n'a aucune définition » —
     * parce qu'un 400 sec devant dix lignes oblige à les relire une par une.
     */
    
    @ExceptionHandler(FmeaScaleValidationException.class)
    public ProblemDetail handleFmeaScaleValidation(FmeaScaleValidationException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "fmea-scale-invalid", "Invalid FMEA Rating Scale");
    }
}
