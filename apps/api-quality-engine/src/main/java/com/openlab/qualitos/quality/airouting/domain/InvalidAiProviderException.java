package com.openlab.qualitos.quality.airouting.domain;

/**
 * Le provider IA demandé n'appartient pas à l'ensemble autorisé
 * {ollama, anthropic, mistral} (ADR 0102). Mappé en 400 (validation).
 */
public class InvalidAiProviderException extends RuntimeException {

    public InvalidAiProviderException(String raw) {
        super("Provider IA invalide: " + raw + " (attendu: ollama|anthropic|mistral)");
    }
}
