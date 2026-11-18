package com.openlab.qualitos.quality.ai;

import com.openlab.qualitos.quality.common.ProblemDetails;
import com.openlab.qualitos.quality.ai.guard.AiCircuitOpenException;
import com.openlab.qualitos.quality.ai.guard.AiPromptTooLargeException;
import com.openlab.qualitos.quality.ai.guard.AiQuotaExceededException;
import com.openlab.qualitos.quality.ai.guard.AiRateLimitExceededException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

/**
 * Les refus du module ai, dits par le module lui-meme.
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
public class AiExceptionHandler {


    // --- Garde-fous IA (OWASP LLM04 — Model Denial of Service) ---

    
    @ExceptionHandler(AiPromptTooLargeException.class)
    public ProblemDetail handleAiPromptTooLarge(AiPromptTooLargeException ex) {
        return ProblemDetails.of(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage(),
                "ai-prompt-too-large", "AI Prompt Too Large");
    }


    @ExceptionHandler(AiRateLimitExceededException.class)
    public ProblemDetail handleAiRateLimit(AiRateLimitExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-rate-limit-exceeded"));
        problem.setTitle("AI Rate Limit Exceeded");
        problem.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiQuotaExceededException.class)
    public ProblemDetail handleAiQuota(AiQuotaExceededException ex) {
        return ProblemDetails.of(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(),
                "ai-quota-exceeded", "AI Quota Exceeded");
    }


    @ExceptionHandler(AiCircuitOpenException.class)
    public ProblemDetail handleAiCircuitOpen(AiCircuitOpenException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setType(URI.create("https://qualitos.io/errors/ai-circuit-open"));
        problem.setTitle("AI Service Unavailable");
        problem.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
