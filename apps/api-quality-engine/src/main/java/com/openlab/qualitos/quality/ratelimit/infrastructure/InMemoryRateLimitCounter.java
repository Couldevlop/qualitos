package com.openlab.qualitos.quality.ratelimit.infrastructure;

import com.openlab.qualitos.quality.ratelimit.domain.RateLimitCounter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Compteur fenêtre fixe en mémoire. Adapté à un déploiement mono-instance V1
 * (sufficient pour SaaS jeune). Pour multi-instance, fournir un adapter Redis
 * et marquer ce stub @Profile("dev") (ou retirer son @Component).
 *
 * Threading : {@link ConcurrentHashMap} + {@link AtomicInteger} → atomique
 * pour increment ; pas de lock global. Auto-cleanup paresseux : à chaque
 * incrément on remplace l'entrée si la fenêtre a tourné.
 */
@Component
public class InMemoryRateLimitCounter implements RateLimitCounter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public Snapshot incrementAndGet(UUID tenantId, String scope, int windowSeconds, Instant now) {
        long start = windowStartEpochSec(now, windowSeconds);
        String key = key(tenantId, scope);
        Bucket b = buckets.compute(key, (k, existing) -> {
            if (existing == null || existing.startEpochSec != start) {
                return new Bucket(start, new AtomicInteger(0));
            }
            return existing;
        });
        int after = b.counter.incrementAndGet();
        return new Snapshot(after, Instant.ofEpochSecond(start + windowSeconds));
    }

    @Override
    public Snapshot peek(UUID tenantId, String scope, int windowSeconds, Instant now) {
        long start = windowStartEpochSec(now, windowSeconds);
        Bucket b = buckets.get(key(tenantId, scope));
        int count = (b != null && b.startEpochSec == start) ? b.counter.get() : 0;
        return new Snapshot(count, Instant.ofEpochSecond(start + windowSeconds));
    }

    /** Visible pour les tests — purge complète. */
    void clear() { buckets.clear(); }

    private static long windowStartEpochSec(Instant now, int windowSeconds) {
        long epoch = now.getEpochSecond();
        return (epoch / windowSeconds) * windowSeconds;
    }

    private static String key(UUID tenantId, String scope) {
        return tenantId.toString() + '' + scope;
    }

    private record Bucket(long startEpochSec, AtomicInteger counter) {}
}
