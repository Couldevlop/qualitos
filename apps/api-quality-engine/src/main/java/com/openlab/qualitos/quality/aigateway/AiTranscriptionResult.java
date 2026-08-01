package com.openlab.qualitos.quality.aigateway;

/**
 * Résultat d'une transcription audio → texte servie par {@code ai-service} (Whisper).
 *
 * @param text       texte transcrit (jamais {@code null} : une transcription vide remonte
 *                   en erreur côté passerelle plutôt que sous forme de chaîne vide)
 * @param language   langue détectée ou imposée ({@code null} si le backend ne la fournit pas)
 * @param durationMs durée de l'audio traité, en millisecondes (0 si non fournie)
 */
public record AiTranscriptionResult(String text, String language, int durationMs) {}
