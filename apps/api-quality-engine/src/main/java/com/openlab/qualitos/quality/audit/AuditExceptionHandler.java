package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module audit, dits par le module lui-meme.
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
public class AuditExceptionHandler {

    @ExceptionHandler(AuditPlanNotFoundException.class)
    public ProblemDetail handleAuditPlanNotFound(AuditPlanNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "audit-plan-not-found", "Audit Plan Not Found");
    }

    @ExceptionHandler(AuditChecklistItemNotFoundException.class)
    public ProblemDetail handleAuditChecklistNotFound(AuditChecklistItemNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "audit-checklist-item-not-found", "Audit Checklist Item Not Found");
    }

    @ExceptionHandler(AuditFindingNotFoundException.class)
    public ProblemDetail handleAuditFindingNotFound(AuditFindingNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "audit-finding-not-found", "Audit Finding Not Found");
    }

    @ExceptionHandler(AuditStateException.class)
    public ProblemDetail handleAuditState(AuditStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "audit-invalid-state", "Invalid Audit State");
    }


    /** 422 : le rapport est incomplet — l'utilisateur sait quoi compléter, rien à recharger. */
    
    @ExceptionHandler(AuditReportIncompleteException.class)
    public ProblemDetail handleAuditReportIncomplete(AuditReportIncompleteException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "audit-report-incomplete", "Incomplete Audit Report");
    }

    @ExceptionHandler(AuditDocumentNotFoundException.class)
    public ProblemDetail handleAuditDocumentNotFound(AuditDocumentNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "audit-document-not-found", "Audit Document Not Found");
    }

    @ExceptionHandler(AuditDocumentValidationException.class)
    public ProblemDetail handleAuditDocumentValidation(AuditDocumentValidationException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "audit-document-invalid", "Invalid Audit Document");
    }

    @ExceptionHandler(AuditDocumentTooLargeException.class)
    public ProblemDetail handleAuditDocumentTooLarge(AuditDocumentTooLargeException ex) {
        return ProblemDetails.of(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(),
                "audit-document-too-large", "Audit Document Too Large");
    }
}
