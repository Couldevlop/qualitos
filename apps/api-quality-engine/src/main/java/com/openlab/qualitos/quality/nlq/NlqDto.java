package com.openlab.qualitos.quality.nlq;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * DTO du module Natural Language Query (CLAUDE.md §7.3). L'utilisateur pose une
 * question en langage naturel ; la passerelle IA ({@code ai-service}) la transforme
 * en SELECT validé (allow-list tables + filtre tenant + rôle PG read-only), l'exécute
 * et renvoie lignes + graphe suggéré + récit.
 */
public final class NlqDto {

    private NlqDto() {}

    /**
     * Requête NLQ. {@code question} bornée (anti-DoS LLM, OWASP LLM04) ; {@code maxRows}
     * plafonné. Le tenant n'est JAMAIS lu ici : il provient du JWT (règle 18.2 #2).
     */
    public record AskRequest(
            @NotBlank @Size(max = 500) String question,
            @Min(1) @Max(1000) Integer maxRows
    ) {}

    /** Réponse NLQ : SQL généré (transparence/explicabilité), résultats et narration. */
    public record AskResponse(
            String question,
            String sql,
            boolean tenantFilterApplied,
            List<String> tablesUsed,
            List<String> functionsUsed,
            List<Map<String, Object>> rows,
            int rowCount,
            double confidence,
            Map<String, Object> chart,
            String narrative
    ) {}
}
