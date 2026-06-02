package com.openlab.qualitos.quality.spc;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * DTO de détection d'anomalies SPC (CLAUDE.md §3.4, §12.1). La série de mesures est
 * envoyée à la passerelle IA ({@code ai-service}) qui calcule les limites de contrôle
 * (carte des valeurs individuelles, σ = MR̄/d2) et applique les 8 règles de Nelson.
 *
 * <p>Le tenant n'est JAMAIS lu ici : il provient du JWT (règle 18.2 #2). La taille de
 * la série est bornée (anti-DoS, OWASP LLM04 — cohérent avec les garde-fous du chemin IA).
 */
public final class SpcDto {

    private SpcDto() {}

    /**
     * Requête SPC. {@code values} borné (1..10000, aligné sur le schéma ai-service).
     * {@code center}/{@code sigma} optionnels : doivent être fournis ensemble (baseline
     * processus connue ; sinon les limites sont estimées depuis la série).
     */
    public record AnalyzeRequest(
            @NotEmpty @Size(max = 10000) List<Double> values,
            Double center,
            @Positive Double sigma
    ) {}

    /** Limites de contrôle calculées (ou fournies). */
    public record Limits(
            double centerLine,
            double sigma,
            double ucl,
            double lcl,
            boolean estimated
    ) {}

    /** Violation détectée par une règle de Nelson (indices de points 0-based). */
    public record Violation(
            String rule,
            String title,
            String description,
            List<Integer> pointIndices,
            String severity
    ) {}

    /** Résultat d'analyse : limites + violations + verdict hors-contrôle. */
    public record AnalyzeResponse(
            int n,
            boolean outOfControl,
            Limits limits,
            List<Violation> violations
    ) {}

    /**
     * Analyse SPC d'un KPI : série tirée de {@code kpi_measurements} (chronologique),
     * + verdict + éventuelle CAPA ouverte sur dérive ({@code capaId} non nul si une CAPA
     * a été créée — procédé hors-contrôle et {@code openCapa=true}).
     */
    public record KpiSpcResponse(
            UUID kpiId,
            String kpiCode,
            String kpiName,
            String unit,
            List<String> periods,
            List<Double> values,
            AnalyzeResponse analysis,
            UUID capaId
    ) {}
}
