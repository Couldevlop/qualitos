package com.openlab.qualitos.quality.pdca;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module pdca, dits par le module lui-meme.
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
public class PdcaExceptionHandler {

    @ExceptionHandler(PdcaCycleNotFoundException.class)
    public ProblemDetail handleCycleNotFound(PdcaCycleNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "pdca-cycle-not-found", "PDCA Cycle Not Found");
    }

    @ExceptionHandler(PdcaStepNotFoundException.class)
    public ProblemDetail handleStepNotFound(PdcaStepNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "pdca-step-not-found", "PDCA Step Not Found");
    }

    @ExceptionHandler(PdcaStateException.class)
    public ProblemDetail handlePdcaState(PdcaStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "pdca-invalid-transition", "Invalid PDCA State Transition");
    }


    // --- preuves d'étape PDCA (ADR 0061) -------------------------------------
    // Types distincts de ceux des preuves CAPA, et non partagés : un client qui
    // lit le champ « type » du ProblemDetail doit pouvoir dire de quel module
    // vient le refus sans avoir à recouper l'URL appelée.

    
    @ExceptionHandler(PdcaStepEvidenceNotFoundException.class)
    public ProblemDetail handlePdcaStepEvidenceNotFound(PdcaStepEvidenceNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "pdca-step-evidence-not-found", "PDCA Step Evidence Not Found");
    }

    @ExceptionHandler(PdcaStepEvidenceValidationException.class)
    public ProblemDetail handlePdcaStepEvidenceValidation(PdcaStepEvidenceValidationException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "pdca-step-evidence-invalid", "Invalid PDCA Step Evidence");
    }

    @ExceptionHandler(PdcaStepEvidenceTooLargeException.class)
    public ProblemDetail handlePdcaStepEvidenceTooLarge(PdcaStepEvidenceTooLargeException ex) {
        return ProblemDetails.of(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(),
                "pdca-step-evidence-too-large", "PDCA Step Evidence Too Large");
    }
}
