package com.openlab.qualitos.quality.apqp;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module apqp, dits par le module lui-meme.
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
public class ApqpExceptionHandler {

    @ExceptionHandler({ApqpProjectNotFoundException.class, ApqpPhaseNotFoundException.class, ApqpDeliverableNotFoundException.class})
    public ProblemDetail handleApqpNotFound(RuntimeException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "apqp-not-found", "APQP Element Not Found");
    }


    /**
     * 422 et non 400 : la requête est bien formée — une liste d'identifiants —
     * mais elle ne décrit pas le cycle. C'est le CONTENU qui est refusé.
     */
    
    @ExceptionHandler(ApqpReorderException.class)
    public ProblemDetail handleApqpReorder(ApqpReorderException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "apqp-invalid-reorder", "Invalid APQP Reorder");
    }


    /**
     * 422 : la requête est bien formée, mais son contenu ne tient pas debout pour
     * ce livrable — une liste de mesures sur une pièce jointe, un renvoi qui ne
     * désigne aucun enregistrement du client.
     */
    
    @ExceptionHandler(ApqpDeliverableValidationException.class)
    public ProblemDetail handleApqpDeliverableValidation(ApqpDeliverableValidationException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "apqp-invalid-deliverable", "Invalid APQP Deliverable Content");
    }

    @ExceptionHandler(ApqpDeliverableEvidenceNotFoundException.class)
    public ProblemDetail handleApqpEvidenceNotFound(ApqpDeliverableEvidenceNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "apqp-evidence-not-found", "APQP Evidence Not Found");
    }

    @ExceptionHandler(ApqpDeliverableEvidenceValidationException.class)
    public ProblemDetail handleApqpEvidenceValidation(ApqpDeliverableEvidenceValidationException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "apqp-invalid-evidence", "Invalid APQP Evidence");
    }


    /**
     * 413 et non 400 : la requête est valide, c'est sa TAILLE qui est refusée — par
     * fichier, par livrable, ou pour le cycle entier. Le message dit lequel des
     * trois plafonds a parlé, sans quoi l'utilisateur réessaie avec un fichier plus
     * petit alors que c'est le cinquième.
     */
    
    @ExceptionHandler(ApqpDeliverableEvidenceTooLargeException.class)
    public ProblemDetail handleApqpEvidenceTooLarge(ApqpDeliverableEvidenceTooLargeException ex) {
        return ProblemDetails.of(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(),
                "apqp-evidence-too-large", "APQP Evidence Too Large");
    }
}
