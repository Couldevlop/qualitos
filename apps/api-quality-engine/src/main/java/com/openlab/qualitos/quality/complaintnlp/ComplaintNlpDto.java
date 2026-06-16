package com.openlab.qualitos.quality.complaintnlp;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * DTO de l'analyse NLP des réclamations clients (CLAUDE.md §4.9, §12.1). Le lot de textes est
 * envoyé à la passerelle IA ({@code ai-service}) qui calcule, par réclamation, le sentiment
 * (lexique pondéré + négation), la catégorie (termes-graines) et la criticité.
 *
 * <p>Le tenant n'est JAMAIS lu ici : il provient du JWT (règle 18.2 #2). La taille de l'entrée
 * est bornée (anti-DoS, OWASP LLM04 — cohérent avec les garde-fous du chemin IA).
 */
public final class ComplaintNlpDto {

    private ComplaintNlpDto() {}

    /**
     * Requête d'analyse. {@code texts} borné (1..2000, aligné sur le schéma ai-service).
     * {@code categories} : taxonomie optionnelle {catégorie: [termes-graines]} (null = défaut).
     */
    public record AnalyzeRequest(
            @NotEmpty @Size(max = 2000) List<String> texts,
            Map<String, List<String>> categories
    ) {}

    /** Analyse d'une réclamation : sentiment, catégorie, criticité. */
    public record Insight(
            int index,
            double sentiment,
            String sentimentLabel,
            String category,
            boolean critical
    ) {}

    /** Résultat : analyses par réclamation + nombre de réclamations critiques. */
    public record AnalyzeResponse(
            int n,
            int criticalCount,
            List<Insight> insights
    ) {}
}
