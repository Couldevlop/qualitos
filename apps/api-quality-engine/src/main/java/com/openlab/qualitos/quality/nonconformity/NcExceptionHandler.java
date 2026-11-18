package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.common.ProblemDetails;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDReportNotFoundException;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDStateException;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDValidationException;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Les refus du module nonconformity, dits par le module lui-meme.
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
public class NcExceptionHandler {

    @ExceptionHandler(NcNotFoundException.class)
    public ProblemDetail handleNcNotFound(NcNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "non-conformity-not-found", "Non-Conformity Not Found");
    }

    @ExceptionHandler(NcStateException.class)
    public ProblemDetail handleNcState(NcStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "non-conformity-invalid-state", "Invalid Non-Conformity State");
    }

    @ExceptionHandler(NcValidationException.class)
    public ProblemDetail handleNcValidation(NcValidationException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "non-conformity-invalid", "Invalid Non-Conformity Data");
    }

    @ExceptionHandler(EightDReportNotFoundException.class)
    public ProblemDetail handleEightDNotFound(EightDReportNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "eightd-report-not-found", "8D Report Not Found");
    }


    /**
     * Émettre avant la clôture, modifier un rapport scellé, télécharger un document
     * qui n'existe pas encore : un conflit d'état, pas une requête malformée.
     */
    
    @ExceptionHandler(EightDStateException.class)
    public ProblemDetail handleEightDState(EightDStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "eightd-report-invalid-state", "Invalid 8D Report State");
    }


    @ExceptionHandler(EightDValidationException.class)
    public ProblemDetail handleEightDValidation(EightDValidationException ex) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/eightd-report-invalid"));
        problem.setTitle("Invalid 8D Report Content");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NcPhotoNotFoundException.class)
    public ProblemDetail handleNcPhotoNotFound(NcPhotoNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "non-conformity-photo-not-found", "Non-Conformity Photo Not Found");
    }

    @ExceptionHandler(NcPhotoValidationException.class)
    public ProblemDetail handleNcPhotoValidation(NcPhotoValidationException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, ex.getMessage(),
                "non-conformity-photo-invalid", "Invalid Non-Conformity Photo");
    }

    @ExceptionHandler(NcPhotoTooLargeException.class)
    public ProblemDetail handleNcPhotoTooLarge(NcPhotoTooLargeException ex) {
        return ProblemDetails.of(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(),
                "non-conformity-photo-too-large", "Non-Conformity Photo Too Large");
    }

    @ExceptionHandler(StorageDisabledException.class)
    public ProblemDetail handleStorageDisabled(StorageDisabledException ex) {
        return ProblemDetails.of(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(),
                "storage-disabled", "Binary Storage Disabled");
    }
}
