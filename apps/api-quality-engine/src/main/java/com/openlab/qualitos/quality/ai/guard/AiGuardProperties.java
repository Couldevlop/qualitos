package com.openlab.qualitos.quality.ai.guard;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Paramètres des garde-fous IA (OWASP LLM04). Externalisés — aucune valeur codée
 * en dur (§18.2-9). Surchargés par tenant/déploiement via {@code qualitos.ai.guard.*}.
 */
@Component
@ConfigurationProperties(prefix = "qualitos.ai.guard")
public class AiGuardProperties {

    /** Active les garde-fous. Si {@code false}, {@link AiGuard#check} est un no-op. */
    private boolean enabled = true;

    /** Débit maximal d'appels IA par tenant et par minute (capacité du bucket). */
    private int requestsPerMinute = 20;

    /** Quota journalier d'appels IA par tenant. */
    private int dailyQuota = 500;

    /** Taille maximale du prompt (caractères, système + utilisateur). */
    private int maxPromptChars = 20_000;

    /** Nombre d'échecs consécutifs ouvrant le disjoncteur. */
    private int circuitFailureThreshold = 5;

    /** Durée d'ouverture du disjoncteur avant passage en demi-ouvert (secondes). */
    private long circuitOpenSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public int getDailyQuota() {
        return dailyQuota;
    }

    public void setDailyQuota(int dailyQuota) {
        this.dailyQuota = dailyQuota;
    }

    public int getMaxPromptChars() {
        return maxPromptChars;
    }

    public void setMaxPromptChars(int maxPromptChars) {
        this.maxPromptChars = maxPromptChars;
    }

    public int getCircuitFailureThreshold() {
        return circuitFailureThreshold;
    }

    public void setCircuitFailureThreshold(int circuitFailureThreshold) {
        this.circuitFailureThreshold = circuitFailureThreshold;
    }

    public long getCircuitOpenSeconds() {
        return circuitOpenSeconds;
    }

    public void setCircuitOpenSeconds(long circuitOpenSeconds) {
        this.circuitOpenSeconds = circuitOpenSeconds;
    }
}
