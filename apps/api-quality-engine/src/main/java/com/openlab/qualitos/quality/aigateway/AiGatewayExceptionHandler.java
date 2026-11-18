package com.openlab.qualitos.quality.aigateway;

import com.openlab.qualitos.quality.common.ProblemDetails;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Les refus du module aigateway, dits par le module lui-meme.
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
public class AiGatewayExceptionHandler {

    @ExceptionHandler(AiGatewayException.class)
    public ProblemDetail handleAiGateway(AiGatewayException ex) {
        return ProblemDetails.of(HttpStatus.BAD_GATEWAY, ex.getMessage(),
                "ai-gateway-unavailable", "AI Gateway Unavailable");
    }


    /**
     * La passerelle IA a répondu et refuse la requête. 422, pas 502 : le service va
     * bien, c'est la demande qui ne passe pas, et l'appelant peut la corriger.
     */
    
    @ExceptionHandler(AiRequestRejectedException.class)
    public ProblemDetail handleAiRequestRejected(AiRequestRejectedException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "ai-request-rejected", "AI Request Rejected");
    }

    @ExceptionHandler(NlqUntranslatableException.class)
    public ProblemDetail handleNlqUntranslatable(NlqUntranslatableException ex) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(),
                "nlq-untranslatable", "NLQ Question Untranslatable");
    }
}
