package com.openlab.qualitos.quality.ratelimit.application;

import com.openlab.qualitos.quality.ratelimit.domain.RateLimitCounter;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitDecision;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicy;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicyNotFoundException;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicyRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Use cases rate-limit.
 *
 * Sécurité (OWASP A04/A07) :
 *  - tryAcquire est l'unique point d'entrée pour décision : il consulte la
 *    politique (ou laisse passer si rien), incrémente le compteur, et tranche.
 *  - Toute exception sur la couche counter (Redis down, etc.) doit fail-open
 *    ou fail-close selon politique. V1 : fail-open (compteur en-mémoire, donc
 *    pas de panne possible). Le caller peut ré-implémenter fail-close si besoin.
 *  - Le scope est validé par RateLimitPolicy (regex), donc impossible
 *    d'injecter quoi que ce soit dans la clé du compteur.
 */
public class RateLimitService {

    private final RateLimitPolicyRepository policies;
    private final RateLimitCounter counter;
    private final TenantProvider tenantProvider;
    private final Clock clock;

    public RateLimitService(RateLimitPolicyRepository policies,
                            RateLimitCounter counter,
                            TenantProvider tenantProvider,
                            Clock clock) {
        this.policies = policies;
        this.counter = counter;
        this.tenantProvider = tenantProvider;
        this.clock = clock;
    }

    /** Vérifie + incrémente le compteur. Appelé par un filtre HTTP / gateway. */
    public RateLimitDecision tryAcquire(UUID tenantId, String scope) {
        if (tenantId == null || scope == null) return RateLimitDecision.unlimited();
        Optional<RateLimitPolicy> opt = policies.findEnabled(tenantId, scope);
        if (opt.isEmpty()) return RateLimitDecision.unlimited();
        RateLimitPolicy p = opt.get();
        Instant now = Instant.now(clock);
        RateLimitCounter.Snapshot snap = counter.incrementAndGet(
                tenantId, scope, p.getWindowSeconds(), now);
        int resetSec = (int) Math.max(0, snap.windowEnd().getEpochSecond() - now.getEpochSecond());
        if (snap.count() > p.getMaxRequests()) {
            return RateLimitDecision.deny(p.getMaxRequests(), resetSec, resetSec);
        }
        return RateLimitDecision.allow(
                p.getMaxRequests(), p.getMaxRequests() - snap.count(), resetSec);
    }

    /** Lecture sans incrément — admin / introspection. */
    public RateLimitDecision peek(UUID tenantId, String scope) {
        if (tenantId == null || scope == null) return RateLimitDecision.unlimited();
        Optional<RateLimitPolicy> opt = policies.findEnabled(tenantId, scope);
        if (opt.isEmpty()) return RateLimitDecision.unlimited();
        RateLimitPolicy p = opt.get();
        Instant now = Instant.now(clock);
        RateLimitCounter.Snapshot snap = counter.peek(
                tenantId, scope, p.getWindowSeconds(), now);
        int resetSec = (int) Math.max(0, snap.windowEnd().getEpochSecond() - now.getEpochSecond());
        boolean allowed = snap.count() < p.getMaxRequests();
        if (!allowed) return RateLimitDecision.deny(p.getMaxRequests(), resetSec, resetSec);
        return RateLimitDecision.allow(
                p.getMaxRequests(), p.getMaxRequests() - snap.count(), resetSec);
    }

    // ----- Admin -----

    public RateLimitDto.PolicyView upsert(RateLimitDto.UpsertPolicyRequest req) {
        UUID tenantId = tenantProvider.requireTenantId();
        Instant now = Instant.now(clock);
        Optional<RateLimitPolicy> existing = policies.findEnabled(tenantId, req.scope());
        RateLimitPolicy p;
        if (existing.isPresent()) {
            p = existing.get();
            p.update(req.windowSeconds(), req.maxRequests(), req.enabled(), now);
        } else {
            p = RateLimitPolicy.create(tenantId, req.scope(),
                    req.windowSeconds(), req.maxRequests(), now);
            if (!req.enabled()) p.update(req.windowSeconds(), req.maxRequests(), false, now);
        }
        return RateLimitDto.PolicyView.of(policies.save(p));
    }

    public List<RateLimitDto.PolicyView> list() {
        UUID tenantId = tenantProvider.requireTenantId();
        return policies.findAllByTenantId(tenantId).stream()
                .map(RateLimitDto.PolicyView::of).toList();
    }

    public RateLimitDto.PolicyView get(UUID id) {
        return RateLimitDto.PolicyView.of(loadForTenant(id));
    }

    public void delete(UUID id) {
        RateLimitPolicy p = loadForTenant(id);
        policies.delete(p);
    }

    private RateLimitPolicy loadForTenant(UUID id) {
        UUID tenantId = tenantProvider.requireTenantId();
        RateLimitPolicy p = policies.findById(id)
                .orElseThrow(() -> new RateLimitPolicyNotFoundException(id));
        if (!p.getTenantId().equals(tenantId)) {
            throw new RateLimitPolicyNotFoundException(id);
        }
        return p;
    }
}
