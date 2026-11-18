package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module capa, dits par le module lui-meme.
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
public class CapaExceptionHandler {

    @ExceptionHandler(CapaNotFoundException.class)
    public ProblemDetail handleCapaNotFound(CapaNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "capa-not-found", "CAPA Not Found");
    }

    @ExceptionHandler(CapaActionNotFoundException.class)
    public ProblemDetail handleCapaActionNotFound(CapaActionNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "capa-action-not-found", "CAPA Action Not Found");
    }

    @ExceptionHandler(CapaStateException.class)
    public ProblemDetail handleCapaState(CapaStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "capa-invalid-state", "Invalid CAPA State");
    }


    /**
     * Saisie refusée sur un dossier ou une action (400), distincte du conflit
     * d'état (409). La mise à jour d'une action est un PATCH non validé par
     * Jakarta — un champ absent doit rester intouché — donc les bornes du
     * libellé sont tenues par le service, et leur refus doit sortir en 400 :
     * sans ce gestionnaire il tomberait dans l'attrape-tout et l'API annoncerait
     * une panne là où l'utilisateur a simplement vidé un champ.
     */
    
    @ExceptionHandler(CapaValidationException.class)
    public ProblemDetail handleCapaValidation(CapaValidationException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "capa-invalid-input", "Invalid CAPA Input");
    }

    @ExceptionHandler(CapaEvidenceNotFoundException.class)
    public ProblemDetail handleCapaEvidenceNotFound(CapaEvidenceNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "capa-evidence-not-found", "CAPA Evidence Not Found");
    }

    @ExceptionHandler(CapaEvidenceValidationException.class)
    public ProblemDetail handleCapaEvidenceValidation(CapaEvidenceValidationException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "capa-evidence-invalid", "Invalid CAPA Evidence");
    }

    @ExceptionHandler(CapaEvidenceTooLargeException.class)
    public ProblemDetail handleCapaEvidenceTooLarge(CapaEvidenceTooLargeException ex) {
        return ProblemDetails.of(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(),
                "capa-evidence-too-large", "CAPA Evidence Too Large");
    }
}
