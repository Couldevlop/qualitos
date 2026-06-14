package com.openlab.qualitos.quality.anomaly;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de détection d'anomalies non-supervisée multivariée (CLAUDE.md §3.4, §12.1).
 * La matrice (échantillons × features) est envoyée à la passerelle IA ({@code ai-service})
 * qui exécute soit un <b>Isolation Forest</b>, soit une <b>reconstruction par ACP</b>
 * (auto-encodeur linéaire) en NumPy pur, et renvoie un score d'anomalie par échantillon.
 *
 * <p>Le tenant n'est JAMAIS lu ici : il provient du JWT (règle 18.2 #2). La taille de la
 * matrice est bornée (anti-DoS, OWASP LLM04 — cohérent avec les garde-fous du chemin IA).
 */
public final class AnomalyDto {

    private AnomalyDto() {}

    /**
     * Requête de détection. {@code samples} borné (1..50000, aligné sur le schéma
     * ai-service). {@code method} : {@code isolation_forest} (défaut) ou
     * {@code reconstruction}. {@code contamination} ∈ (0, 0.5] (fraction d'anomalies
     * attendue) ; {@code threshold} optionnel (seuil explicite sur le score, sinon
     * quantile de contamination). Optionnels nuls non transmis à la passerelle.
     */
    public record DetectRequest(
            @NotEmpty @Size(max = 50000) List<List<Double>> samples,
            String method,
            @DecimalMin(value = "0.0", inclusive = false) @DecimalMax("0.5") Double contamination,
            Double threshold
    ) {}

    /** Score d'anomalie d'un échantillon (index 0-based dans la matrice d'entrée). */
    public record Point(
            int index,
            double score,
            boolean isAnomaly,
            Integer topFeature
    ) {}

    /** Résultat : scores par point + verdict global + seuil effectif appliqué. */
    public record DetectResponse(
            int n,
            int nFeatures,
            String method,
            double contamination,
            double threshold,
            int anomalyCount,
            boolean hasAnomalies,
            List<Point> points
    ) {}
}
