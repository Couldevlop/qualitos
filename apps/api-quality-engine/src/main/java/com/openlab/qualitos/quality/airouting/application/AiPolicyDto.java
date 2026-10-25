package com.openlab.qualitos.quality.airouting.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Vues et commandes de la politique IA d'un tenant (ADR 0102). */
public final class AiPolicyDto {

    private AiPolicyDto() {}

    public record PinCommand(String provider) {}

    /**
     * @param pinned            le tenant a sa propre politique
     * @param provider          le fournisseur épinglé, {@code null} sinon
     * @param effectiveProvider celui qui répond réellement : l'épinglé, ou le défaut de la
     *                          plateforme ; {@code null} si l'ai-service ne répond pas
     * @param platformDefault   le défaut de la plateforme, {@code null} s'il est inconnu
     * @param catalogAvailable  l'ai-service a répondu : les {@code configured} sont fiables
     * @param providers         les choix possibles, dans un ordre stable
     */
    public record View(boolean pinned, String provider, String effectiveProvider, String platformDefault,
                       boolean catalogAvailable, List<ProviderOption> providers,
                       Instant updatedAt, UUID updatedBy) {}

    /**
     * @param onPremise  le modèle tourne chez le client : aucune donnée vers un tiers
     * @param configured {@code null} quand l'ai-service n'a pas répondu
     */
    public record ProviderOption(String name, boolean onPremise, Boolean configured) {}
}
