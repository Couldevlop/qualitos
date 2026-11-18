package com.openlab.qualitos.quality.ideas.web;

import com.openlab.qualitos.quality.common.ProblemDetails;
import com.openlab.qualitos.quality.ideas.domain.IdeaNotFoundException;
import com.openlab.qualitos.quality.ideas.domain.IdeaStateException;
import com.openlab.qualitos.quality.ideas.domain.VoteClosedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module ideas, dits par le module lui-meme.
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
public class IdeaExceptionHandler {

    @ExceptionHandler(IdeaNotFoundException.class)
    public ProblemDetail handleIdeaNotFound(IdeaNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, ex.getMessage(),
                "idea-not-found", "Idea Not Found");
    }


    /**
     * 409 : la demande est licite, c'est le moment qui ne l'est plus. L'idée a
     * été tranchée, et le compteur doit rester celui du jour de la décision.
     */
    
    @ExceptionHandler(VoteClosedException.class)
    public ProblemDetail handleVoteClosed(VoteClosedException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, ex.getMessage(),
                "vote-closed", "Vote Closed");
    }


    /** 422 : la requête est bien formée, mais la transition demandée n'existe pas d'ici. */
    
    @ExceptionHandler(IdeaStateException.class)
    public ProblemDetail handleIdeaState(IdeaStateException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "idea-invalid-transition", "Invalid Idea Transition");
    }
}
