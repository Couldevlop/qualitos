package com.openlab.qualitos.quality.nccluster;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO de clustering de non-conformités (CLAUDE.md §4.3, §12.1). La liste de textes de NC est
 * envoyée à la passerelle IA ({@code ai-service}) qui applique TF-IDF + DBSCAN (densité,
 * NumPy pur) et renvoie des clusters de NC similaires (patterns récurrents) + le bruit.
 *
 * <p>Le tenant n'est JAMAIS lu ici : il provient du JWT (règle 18.2 #2). La taille de l'entrée
 * est bornée (anti-DoS, OWASP LLM04 — cohérent avec les garde-fous du chemin IA).
 */
public final class NcClusterDto {

    private NcClusterDto() {}

    /**
     * Requête de clustering. {@code texts} borné (2..2000, aligné sur le schéma ai-service).
     * {@code threshold} : similarité cosinus minimale du voisinage (optionnel). {@code minSamples} :
     * taille minimale du voisinage d'un point-cœur DBSCAN (densité ; optionnel).
     */
    public record ClusterRequest(
            @NotEmpty @Size(min = 2, max = 2000) List<String> texts,
            Double threshold,
            @Min(2) @Max(100) Integer minSamples
    ) {}

    /** Un cluster : positions des NC membres + termes représentatifs (explicabilité). */
    public record Cluster(
            int clusterId,
            List<Integer> indices,
            int size,
            List<String> topTerms
    ) {}

    /** Résultat : clusters + bruit + ratio regroupé + algorithme appliqué. */
    public record ClusterResponse(
            int n,
            double clusteredRatio,
            String method,
            List<Cluster> clusters,
            List<Integer> noiseIndices
    ) {}
}
