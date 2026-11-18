package com.openlab.qualitos.quality.fives;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module fives, dits par le module lui-meme.
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
public class FiveSExceptionHandler {

    @ExceptionHandler(FiveSAuditNotFoundException.class)
    public ProblemDetail handleFiveSNotFound(FiveSAuditNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "fives-audit-not-found", "5S Audit Not Found");
    }

    @ExceptionHandler(FiveSStateException.class)
    public ProblemDetail handleFiveSState(FiveSStateException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "fives-invalid-state", "Invalid 5S Audit State");
    }

    @ExceptionHandler(FiveSItemPhotoNotFoundException.class)
    public ProblemDetail handleFiveSPhotoNotFound(FiveSItemPhotoNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "fives-photo-not-found", "5S Photo Not Found");
    }

    @ExceptionHandler(FiveSItemPhotoValidationException.class)
    public ProblemDetail handleFiveSPhotoValidation(FiveSItemPhotoValidationException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "fives-invalid-photo", "Invalid 5S Photo");
    }


    /**
     * 413 et non 400 : la requête est valide, c'est sa TAILLE qui est refusée — par
     * photo, par pilier, ou pour l'audit entier. Le message dit lequel des trois
     * plafonds a parlé, sans quoi l'auditeur recommence avec une photo plus petite
     * alors que c'est la sixième du pilier.
     */
    
    @ExceptionHandler(FiveSItemPhotoTooLargeException.class)
    public ProblemDetail handleFiveSPhotoTooLarge(FiveSItemPhotoTooLargeException ex) {
        return ProblemDetails.of(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(),
                "fives-photo-too-large", "5S Photo Too Large");
    }
}
