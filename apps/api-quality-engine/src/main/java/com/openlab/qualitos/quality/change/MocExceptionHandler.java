package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.common.ProblemDetails;
import com.openlab.qualitos.quality.change.domain.MocException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Les refus du module change, dits par le module lui-meme.
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
public class MocExceptionHandler {

    @ExceptionHandler(ChangeRequestNotFoundException.class)
    public ProblemDetail handleChangeRequestNotFound(ChangeRequestNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "change-request-not-found", "Change Request Not Found");
    }


    /**
     * Les refus de la gestion des changements (ADR 0087) : le statut suit la
     * nature du refus, et {@code code} (stable) permet à l'écran de l'expliquer
     * dans la langue de l'utilisateur sans afficher le message du serveur.
     */
    @ExceptionHandler(MocException.class)
    public ProblemDetail handleMoc(MocException ex) {
        HttpStatus status = switch (ex.kind()) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/moc"));
        problem.setTitle("Change Management");
        problem.setProperty("code", ex.code());
        ex.details().forEach(problem::setProperty);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
