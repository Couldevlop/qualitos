package com.openlab.qualitos.quality.ratelimit.infrastructure;

import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicy;
import com.openlab.qualitos.quality.ratelimit.domain.RateLimitPolicyRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RateLimitPolicyRepositoryAdapter implements RateLimitPolicyRepository {

    private final RateLimitPolicyJpaRepository jpa;

    public RateLimitPolicyRepositoryAdapter(RateLimitPolicyJpaRepository jpa) { this.jpa = jpa; }

    @Override
    @Transactional
    public RateLimitPolicy save(RateLimitPolicy p) {
        RateLimitPolicyJpaEntity existing = p.getId() != null
                ? jpa.findById(p.getId()).orElse(null) : null;
        RateLimitPolicyJpaEntity e = existing != null ? existing : new RateLimitPolicyJpaEntity();
        if (p.getId() != null) e.setId(p.getId());
        e.setTenantId(p.getTenantId());
        e.setScope(p.getScope());
        e.setWindowSeconds(p.getWindowSeconds());
        e.setMaxRequests(p.getMaxRequests());
        e.setEnabled(p.isEnabled());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        RateLimitPolicyJpaEntity saved = jpa.save(e);
        if (p.getId() == null) p.assignId(saved.getId());
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RateLimitPolicy> findById(UUID id) {
        return jpa.findById(id).map(RateLimitPolicyRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RateLimitPolicy> findEnabled(UUID tenantId, String scope) {
        return jpa.findByTenantIdAndScopeAndEnabledTrue(tenantId, scope)
                .map(RateLimitPolicyRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RateLimitPolicy> findAllByTenantId(UUID tenantId) {
        return jpa.findByTenantIdOrderByScopeAsc(tenantId).stream()
                .map(RateLimitPolicyRepositoryAdapter::toDomain).toList();
    }

    @Override
    @Transactional
    public void delete(RateLimitPolicy p) {
        if (p.getId() != null) jpa.deleteById(p.getId());
    }

    static RateLimitPolicy toDomain(RateLimitPolicyJpaEntity e) {
        return new RateLimitPolicy(e.getId(), e.getTenantId(), e.getScope(),
                e.getWindowSeconds(), e.getMaxRequests(), e.isEnabled(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
