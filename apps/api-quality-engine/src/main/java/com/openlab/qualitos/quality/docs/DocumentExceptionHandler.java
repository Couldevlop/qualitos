package com.openlab.qualitos.quality.docs;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module docs, dits par le module lui-meme.
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
public class DocumentExceptionHandler {

    @ExceptionHandler(DocumentNotFoundException.class)
    public ProblemDetail handleDocNotFound(DocumentNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "document-not-found", "Document Not Found");
    }

    @ExceptionHandler(DocumentVersionNotFoundException.class)
    public ProblemDetail handleVersionNotFound(DocumentVersionNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "document-version-not-found", "Document Version Not Found");
    }

    @ExceptionHandler(DocumentSignatoryException.class)
    public ProblemDetail handleDocSignatory(DocumentSignatoryException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "document-invalid-signatories", "Invalid Signatories");
    }

    @ExceptionHandler(DocumentStateException.class)
    public ProblemDetail handleDocState(DocumentStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "document-invalid-state", "Invalid Document State");
    }

    @ExceptionHandler(DocumentCodeConflictException.class)
    public ProblemDetail handleDocCodeConflict(DocumentCodeConflictException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "document-code-conflict", "Document Code Conflict");
    }

    @ExceptionHandler(DocumentFileRejectedException.class)
    public ProblemDetail handleDocFileRejected(DocumentFileRejectedException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "document-file-invalid", "Invalid Document File");
    }

    @ExceptionHandler(DocumentFileTooLargeException.class)
    public ProblemDetail handleDocFileTooLarge(DocumentFileTooLargeException ex) {
        return ProblemDetails.of(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(),
                "document-file-too-large", "Document File Too Large");
    }

    @ExceptionHandler(DocumentCatalogNotFoundException.class)
    public ProblemDetail handleDocCatalogNotFound(DocumentCatalogNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "document-catalog-not-found", "Document Folder or Process Not Found");
    }
}
