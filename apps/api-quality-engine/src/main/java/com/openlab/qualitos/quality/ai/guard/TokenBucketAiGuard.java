package com.openlab.qualitos.quality.ai.guard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptateur en mémoire des garde-fous IA (OWASP LLM04), cloisonné par tenant.
 *
 * <p>Combine, par tenant et sans dépendance externe (zéro surface CVE, OWASP A06) :
 * <ul>
 *   <li>un <b>token bucket</b> à remplissage continu (débit/minute) ;</li>
 *   <li>un <b>compteur de quota journalier</b> remis à zéro au changement de jour ;</li>
 *   <li>un <b>disjoncteur</b> CLOSED → OPEN → HALF_OPEN.</li>
 * </ul>
 *
 * <p>L'état d'un tenant est encapsulé dans {@link TenantState} et toute mutation
 * passe par un bloc {@code synchronized(state)} : O(1), contention limitée au tenant.
 * L'horloge est injectable pour des tests déterministes.
 *
 * <p>Limite assumée : l'état est local au nœud (non partagé entre répliques). Pour
 * un cloisonnement strict en cluster, fournir une implémentation Redis du port
 * {@link AiGuard} — les appelants sont inchangés.
 */
@Component
public class TokenBucketAiGuard implements AiGuard {

    private static final Logger log = LoggerFactory.getLogger(TokenBucketAiGuard.class);

    private final AiGuardProperties props;
    private final Clock clock;
    private final ConcurrentHashMap<String, TenantState> states = new ConcurrentHashMap<>();

    @Autowired
    public TokenBucketAiGuard(AiGuardProperties props) {
        this(props, Clock.systemUTC());
    }

    /** Constructeur de test : horloge contrôlée. */
    TokenBucketAiGuard(AiGuardProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
    }

    @Override
    public void check(AiCallContext context) {
        if (!props.isEnabled()) {
            return;
        }
        // 1) Taille du prompt — validation d'entrée, avant tout état.
        if (context.promptChars() > props.getMaxPromptChars()) {
            throw new AiPromptTooLargeException(
                    "Prompt trop volumineux (" + context.promptChars() + " > "
                            + props.getMaxPromptChars() + " caractères).");
        }

        TenantState state = states.computeIfAbsent(context.tenantId(),
                k -> new TenantState(props.getRequestsPerMinute(), now()));
        synchronized (state) {
            long now = now();

            // 2) Disjoncteur (fail-fast si le service IA est en panne).
            if (state.circuit == Circuit.OPEN) {
                long elapsed = now - state.openedAtMillis;
                long openMs = props.getCircuitOpenSeconds() * 1000L;
                if (elapsed < openMs) {
                    throw new AiCircuitOpenException(
                            "Service IA momentanément indisponible (disjoncteur ouvert).",
                            Math.max(1, (openMs - elapsed) / 1000L));
                }
                // Refroidissement écoulé → demi-ouvert : on autorise un appel d'essai.
                state.circuit = Circuit.HALF_OPEN;
                log.debug("Disjoncteur IA demi-ouvert pour tenant {}", context.tenantId());
            }

            // 3) Quota journalier (vérifié sans incrément si le débit échoue ensuite).
            LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
            if (!today.equals(state.quotaDay)) {
                state.quotaDay = today;
                state.quotaUsed = 0;
            }
            if (state.quotaUsed >= props.getDailyQuota()) {
                throw new AiQuotaExceededException(
                        "Quota journalier d'appels IA épuisé (" + props.getDailyQuota() + ").");
            }

            // 4) Débit (token bucket).
            refill(state, now);
            if (state.tokens < 1.0d) {
                double perMin = Math.max(1, props.getRequestsPerMinute());
                long retry = (long) Math.ceil((1.0d - state.tokens) * (60.0d / perMin));
                throw new AiRateLimitExceededException(
                        "Débit d'appels IA dépassé (" + props.getRequestsPerMinute() + "/min).",
                        Math.max(1, retry));
            }

            // Admis : consomme un jeton et incrémente le quota.
            state.tokens -= 1.0d;
            state.quotaUsed++;
        }
    }

    @Override
    public void recordSuccess(String tenantId) {
        if (!props.isEnabled()) {
            return;
        }
        TenantState state = states.get(key(tenantId));
        if (state == null) {
            return;
        }
        synchronized (state) {
            state.consecutiveFailures = 0;
            if (state.circuit != Circuit.CLOSED) {
                log.debug("Disjoncteur IA refermé pour tenant {}", tenantId);
            }
            state.circuit = Circuit.CLOSED;
        }
    }

    @Override
    public void recordFailure(String tenantId) {
        if (!props.isEnabled()) {
            return;
        }
        TenantState state = states.computeIfAbsent(key(tenantId),
                k -> new TenantState(props.getRequestsPerMinute(), now()));
        synchronized (state) {
            state.consecutiveFailures++;
            boolean trip = state.circuit == Circuit.HALF_OPEN
                    || state.consecutiveFailures >= props.getCircuitFailureThreshold();
            if (trip) {
                state.circuit = Circuit.OPEN;
                state.openedAtMillis = now();
                log.warn("Disjoncteur IA ouvert pour tenant {} après {} échec(s)",
                        tenantId, state.consecutiveFailures);
            }
        }
    }

    private void refill(TenantState state, long now) {
        double perMin = Math.max(1, props.getRequestsPerMinute());
        double elapsedSec = (now - state.lastRefillMillis) / 1000.0d;
        if (elapsedSec > 0) {
            state.tokens = Math.min(perMin, state.tokens + elapsedSec * (perMin / 60.0d));
            state.lastRefillMillis = now;
        }
    }

    private long now() {
        return clock.millis();
    }

    private static String key(String tenantId) {
        return (tenantId == null || tenantId.isBlank()) ? "unknown" : tenantId;
    }

    private enum Circuit {CLOSED, OPEN, HALF_OPEN}

    /** État par tenant (débit + quota + disjoncteur). Muté sous {@code synchronized(this)}. */
    private static final class TenantState {
        double tokens;
        long lastRefillMillis;
        int quotaUsed;
        LocalDate quotaDay;
        int consecutiveFailures;
        Circuit circuit = Circuit.CLOSED;
        long openedAtMillis;

        TenantState(int capacity, long now) {
            this.tokens = Math.max(1, capacity);
            this.lastRefillMillis = now;
        }
    }
}
