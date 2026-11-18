package com.openlab.qualitos.quality.itsm;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Les refus du module itsm, dits par le module lui-meme.
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
public class ItsmExceptionHandler {

    @ExceptionHandler(ItsmConnectionNotFoundException.class)
    public ProblemDetail handleItsmNotFound(ItsmConnectionNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "itsm-connection-not-found", "ITSM Connection Not Found");
    }


    @ExceptionHandler(ItsmSyncException.class)
    public ProblemDetail handleItsmSync(ItsmSyncException ex) {
        // 502 = Bad Gateway : appel sortant vers ITSM en erreur.
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/itsm-sync-failed"));
        problem.setTitle("ITSM Sync Failed");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
