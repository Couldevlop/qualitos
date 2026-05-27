package com.openlab.qualitos.quality.aigateway;

/** Résultat d'une complétion LLM via la passerelle IA (ai-service). */
public record AiCompletionResult(
        String text,
        String provider,
        int tokensUsed,
        int latencyMs
) {}
