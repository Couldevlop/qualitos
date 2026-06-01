package com.openlab.qualitos.quality.ai.guard;

/**
 * Port (architecture hexagonale) des garde-fous appliqués au chemin LLM avant
 * tout appel sortant — OWASP LLM Top 10, LLM04 « Model Denial of Service ».
 *
 * <p>Trois protections, toutes cloisonnées par tenant :
 * <ul>
 *   <li><b>Débit</b> (token bucket) et <b>quota journalier</b> : empêchent
 *       l'épuisement du modèle par un tenant.</li>
 *   <li><b>Disjoncteur</b> (circuit breaker) : court-circuite en fail-fast quand
 *       le service IA enchaîne les échecs.</li>
 *   <li><b>Taille de prompt</b> : borne l'entrée (charge abusive).</li>
 * </ul>
 *
 * <p>L'adaptateur par défaut est en mémoire ({@link TokenBucketAiGuard}). Le port
 * permet une implémentation distribuée (Redis) ultérieure sans toucher les appelants.
 */
public interface AiGuard {

    /**
     * Vérifie qu'un appel IA est autorisé pour le contexte donné. Consomme un jeton
     * de débit et incrémente le quota du tenant si l'appel est admis.
     *
     * @throws AiPromptTooLargeException   si le prompt dépasse la taille maximale (413)
     * @throws AiCircuitOpenException      si le disjoncteur du tenant est ouvert (503)
     * @throws AiRateLimitExceededException si le débit par minute est dépassé (429)
     * @throws AiQuotaExceededException    si le quota journalier est épuisé (429)
     */
    void check(AiCallContext context);

    /** Signale un appel IA réussi : referme le disjoncteur du tenant. */
    void recordSuccess(String tenantId);

    /** Signale un appel IA en échec : rapproche/ouvre le disjoncteur du tenant. */
    void recordFailure(String tenantId);
}
