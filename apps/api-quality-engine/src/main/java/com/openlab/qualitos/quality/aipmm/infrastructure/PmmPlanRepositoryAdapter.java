package com.openlab.qualitos.quality.aipmm.infrastructure;

import com.openlab.qualitos.quality.aipmm.application.TenantProvider;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlan;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanRepository;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PmmPlanRepositoryAdapter implements PmmPlanRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final PmmPlanJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public PmmPlanRepositoryAdapter(
            PmmPlanJpaRepository jpa,
            @Qualifier("pmmTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public PmmPlan save(PmmPlan plan) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(plan.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        PmmPlanJpaEntity existing = plan.getId() != null
                ? jpa.findByIdAndTenantId(plan.getId(), currentTenant).orElse(null)
                : null;
        PmmPlanJpaEntity saved = jpa.save(PmmPlanMapper.toEntity(plan, existing));
        PmmPlan out = PmmPlanMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<PmmPlan> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(PmmPlanMapper::toDomain);
    }

    @Override
    public List<PmmPlan> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(PmmPlanMapper::toDomain).getContent();
    }

    @Override
    public List<PmmPlan> findByTenantAndStatus(UUID tenantId, PmmPlanStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(PmmPlanMapper::toDomain).getContent();
    }

    @Override
    public List<PmmPlan> findByTenantAndAiSystemId(UUID tenantId, UUID aiSystemId) {
        return jpa.findByTenantIdAndAiSystemId(tenantId, aiSystemId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(PmmPlanMapper::toDomain).getContent();
    }

    @Override
    public List<PmmPlan> findOverdueReviews(UUID tenantId, Instant now, int limit) {
        return jpa.findOverdueReviews(tenantId, now, limit).stream()
                .map(PmmPlanMapper::toDomain).toList();
    }

    @Override
    public Optional<PmmPlan> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference)
                .map(PmmPlanMapper::toDomain);
    }

    @Override
    public boolean existsByTenantAndReference(UUID tenantId, String reference) {
        return jpa.existsByTenantIdAndReference(tenantId, reference);
    }

    @Override
    public void delete(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        jpa.findByIdAndTenantId(id, tenant).ifPresent(jpa::delete);
    }
}
