package com.openlab.qualitos.quality.forecast;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de prévision KPI (CLAUDE.md §6.5, §12.1). La série chronologique est envoyée à la
 * passerelle IA ({@code ai-service}) qui applique un lissage exponentiel de Holt-Winters
 * (niveau + tendance + saisonnalité optionnelle, NumPy pur) et renvoie une prévision à
 * l'horizon + la probabilité d'atteindre la cible, avec intervalles de prédiction à 95 %.
 *
 * <p>Le tenant n'est JAMAIS lu ici : il provient du JWT (règle 18.2 #2). La taille de la
 * série est bornée (anti-DoS, OWASP LLM04 — cohérent avec les garde-fous du chemin IA).
 */
public final class ForecastDto {

    private ForecastDto() {}

    /**
     * Requête de prévision. {@code values} borné (4..10000, aligné sur le schéma ai-service).
     * {@code direction} : {@code at_least} (atteindre AU MOINS la cible) ou {@code at_most}.
     * {@code seasonalPeriod} optionnel (≥ 2 ; null = pas de saisonnalité, Holt linéaire).
     */
    public record ForecastRequest(
            @NotEmpty @Size(min = 4, max = 10000) List<Double> values,
            @NotNull Double target,
            @Min(1) @Max(60) Integer horizon,
            @Pattern(regexp = "at_least|at_most") String direction,
            @Min(2) @Max(365) Integer seasonalPeriod
    ) {}

    /** Point projeté : prévision + intervalle de prédiction à 95 %. */
    public record Point(
            int step,
            double value,
            double low,
            double high
    ) {}

    /** Résultat : prévision à l'horizon + probabilité d'atteinte + métadonnées d'explicabilité. */
    public record ForecastResponse(
            int n,
            double slope,
            double intercept,
            double residualSigma,
            double r2,
            int horizon,
            double target,
            String direction,
            double probability,
            String confidence,
            String model,
            int seasonalPeriod,
            List<Point> points
    ) {}
}
