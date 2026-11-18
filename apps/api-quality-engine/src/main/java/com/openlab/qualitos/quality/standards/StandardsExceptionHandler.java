package com.openlab.qualitos.quality.standards;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module standards, dits par le module lui-meme.
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
public class StandardsExceptionHandler {

    @ExceptionHandler(StandardNotFoundException.class)
    public ProblemDetail handleStandardNotFound(StandardNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "standard-not-found", "Standard Not Found");
    }

    @ExceptionHandler(ProcedureSourceException.class)
    public ProblemDetail handleProcedureSource(ProcedureSourceException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "procedure-source", "Invalid Procedure Source");
    }

    @ExceptionHandler(StandardCodeConflictException.class)
    public ProblemDetail handleStandardCodeConflict(StandardCodeConflictException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "standard-code-conflict", "Standard Code Conflict");
    }


    /**
     * 403 et non 404 : la norme de la plateforme est visible de tous, son
     * existence n'est pas un secret — la masquer laisserait croire à une faute de
     * frappe. Le référentiel d'un AUTRE tenant, lui, reste un 404.
     */
    
    @ExceptionHandler(PlatformStandardWriteException.class)
    public ProblemDetail handlePlatformStandardWrite(PlatformStandardWriteException ex) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, ex.getMessage(),
                "platform-standard-write", "Platform Standard Is Read-Only");
    }

    @ExceptionHandler(SectionNotFoundException.class)
    public ProblemDetail handleSectionNotFound(SectionNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "standard-section-not-found", "Standard Section Not Found");
    }

    @ExceptionHandler(ClauseNotFoundException.class)
    public ProblemDetail handleClauseNotFound(ClauseNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "standard-clause-not-found", "Standard Clause Not Found");
    }

    @ExceptionHandler(TenantStandardNotFoundException.class)
    public ProblemDetail handleTenantStandardNotFound(TenantStandardNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "tenant-standard-not-found", "Tenant Standard Adoption Not Found");
    }

    @ExceptionHandler(RequirementNotFoundException.class)
    public ProblemDetail handleRequirementNotFound(RequirementNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "standard-requirement-not-found", "Standard Requirement Not Found");
    }

    @ExceptionHandler(EvidenceNotFoundException.class)
    public ProblemDetail handleEvidenceNotFound(EvidenceNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "requirement-evidence-not-found", "Requirement Evidence Not Found");
    }

    @ExceptionHandler(RoadmapStageNotFoundException.class)
    public ProblemDetail handleRoadmapStageNotFound(RoadmapStageNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "certification-roadmap-stage-not-found", "Certification Roadmap Stage Not Found");
    }

    @ExceptionHandler(DocumentTemplateNotFoundException.class)
    public ProblemDetail handleDocumentTemplateNotFound(DocumentTemplateNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "standard-document-template-not-found", "Standard Document Template Not Found");
    }

    @ExceptionHandler(AdoptionConflictException.class)
    public ProblemDetail handleAdoptionConflict(AdoptionConflictException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "adoption-conflict", "Adoption Conflict");
    }

    @ExceptionHandler(AdoptionStateException.class)
    public ProblemDetail handleAdoptionState(AdoptionStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "adoption-invalid-state", "Invalid Adoption State");
    }
}
