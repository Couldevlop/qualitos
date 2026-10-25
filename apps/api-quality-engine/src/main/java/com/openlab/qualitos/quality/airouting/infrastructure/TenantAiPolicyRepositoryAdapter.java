package com.openlab.qualitos.quality.airouting.infrastructure;

import com.openlab.qualitos.quality.airouting.domain.AiProvider;
import com.openlab.qualitos.quality.airouting.domain.TenantAiPolicy;
import com.openlab.qualitos.quality.airouting.domain.TenantAiPolicyRepository;

import java.util.Optional;
import java.util.UUID;

/** Adaptateur JPA du port {@link TenantAiPolicyRepository} (ADR 0102). */
public class TenantAiPolicyRepositoryAdapter implements TenantAiPolicyRepository {

    private final TenantAiPolicyJpaRepository jpa;

    public TenantAiPolicyRepositoryAdapter(TenantAiPolicyJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<TenantAiPolicy> findByTenantId(UUID tenantId) {
        return jpa.findById(tenantId).map(TenantAiPolicyRepositoryAdapter::toDomain);
    }

    @Override
    public TenantAiPolicy save(TenantAiPolicy policy) {
        TenantAiPolicyJpaEntity e = new TenantAiPolicyJpaEntity();
        e.setTenantId(policy.getTenantId());
        e.setProvider(policy.getProvider().wireName());
        e.setUpdatedAt(policy.getUpdatedAt());
        e.setUpdatedBy(policy.getUpdatedBy());
        return toDomain(jpa.save(e));
    }

    @Override
    public void deleteByTenantId(UUID tenantId) {
        jpa.deleteById(tenantId);
    }

    static TenantAiPolicy toDomain(TenantAiPolicyJpaEntity e) {
        return new TenantAiPolicy(e.getTenantId(), AiProvider.parse(e.getProvider()),
                e.getUpdatedAt(), e.getUpdatedBy());
    }
}
