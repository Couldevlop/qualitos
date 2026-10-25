package com.openlab.qualitos.quality.airouting.domain;

import java.util.Locale;

/**
 * Provider LLM cible de la passerelle IA (ADR 0014, ADR 0102).
 *
 * <p>L'identifiant {@link #wireName()} est la valeur exacte attendue par le champ
 * {@code provider} des schémas {@code ai-service} (cf. {@code presentation/provider_defaults.py}).
 * On garde une correspondance 1:1 minuscule pour éviter toute divergence.
 */
public enum AiProvider {

    /** Modèle local on-premise — souveraineté : aucune donnée vers une API externe (§12.2). */
    OLLAMA("ollama"),
    /** API Anthropic (externe). */
    ANTHROPIC("anthropic"),
    /** API Mistral (externe). */
    MISTRAL("mistral");

    private final String wireName;

    AiProvider(String wireName) {
        this.wireName = wireName;
    }

    /**
     * Le modèle tourne dans l'infrastructure du client : ses prompts — et donc les
     * données du tenant qu'ils portent — ne partent vers aucun tiers (§12.2).
     */
    public boolean isOnPremise() {
        return this == OLLAMA;
    }

    /** Nom transmis à ai-service dans le corps de requête (champ {@code provider}). */
    public String wireName() {
        return wireName;
    }

    /**
     * Parse strict (insensible à la casse / espaces) d'un provider. Toute valeur hors
     * {ollama, anthropic, mistral} est rejetée (validation OWASP — pas d'injection de
     * provider arbitraire dans la passerelle).
     *
     * @throws InvalidAiProviderException si la valeur n'est pas reconnue
     */
    public static AiProvider parse(String raw) {
        if (raw == null) {
            throw new InvalidAiProviderException(null);
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (AiProvider p : values()) {
            if (p.wireName.equals(normalized)) {
                return p;
            }
        }
        throw new InvalidAiProviderException(raw);
    }
}
