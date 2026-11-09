package com.openlab.qualitos.quality.spc;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    // ---- prévision de sortie des limites (§6.5) ----

    /**
     * Requête de prévision : jusqu'où projeter, et à partir de quel risque annoncer.
     *
     * <p>Quatre mesures au minimum : un ajustement de tendance sur trois points ne
     * dit rien. {@code horizon} et {@code threshold} restent facultatifs — les
     * défauts (6 périodes, risque 0,5) sont ceux du service de calcul.
     */
    public record ForecastRequest(
            @NotEmpty @Size(min = 4, max = 10000) List<Double> values,
            @Min(1) @Max(60) Integer horizon,
            Double center,
            @Positive Double sigma,
            @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("1.0") Double threshold
    ) {}

    /** Une période à venir : où le procédé se dirige, et le risque qu'il sorte. */
    public record ForecastPoint(
            int step,
            double value,
            double low,
            double high,
            double pAboveUcl,
            double pBelowLcl,
            double pBreach
    ) {}

    /**
     * Prévision de sortie des limites.
     *
     * <p>{@code firstBreachStep} est nul tant que le procédé reste dans ses limites
     * sur tout l'horizon. {@code confidence}, {@code r2} et {@code trend} voyagent
     * avec le chiffre : un risque élevé sur une série courte n'est pas une
     * prédiction, et doit pouvoir se lire comme tel (§12.3).
     */
    public record ForecastResponse(
            int n,
            int horizon,
            double threshold,
            boolean willBreach,
            Integer firstBreachStep,
            String side,
            double trend,
            double residualSigma,
            double r2,
            String confidence,
            String model,
            Limits limits,
            List<ForecastPoint> points
    ) {}

    /**
     * Prévision SPC d'un KPI : la série réelle, la prévision, et l'éventuelle CAPA
     * PRÉVENTIVE ouverte sur l'annonce ({@code capaId} non nul si une CAPA a été
     * créée — sortie prévue et {@code openCapa=true}).
     */
    public record KpiForecastResponse(
            UUID kpiId,
            String kpiCode,
            String kpiName,
            String unit,
            List<String> periods,
            List<Double> values,
            ForecastResponse forecast,
            UUID capaId
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
