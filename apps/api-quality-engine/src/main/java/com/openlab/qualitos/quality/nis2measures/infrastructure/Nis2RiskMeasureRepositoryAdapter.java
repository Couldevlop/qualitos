package com.openlab.qualitos.quality.nis2measures.infrastructure;

import com.openlab.qualitos.quality.nis2measures.application.TenantProvider;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureCategory;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureStatus;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2RiskMeasure;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2RiskMeasureRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class Nis2RiskMeasureRepositoryAdapter implements Nis2RiskMeasureRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final Nis2RiskMeasureJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public Nis2RiskMeasureRepositoryAdapter(
            Nis2RiskMeasureJpaRepository jpa,
            @Qualifier("nis2mTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public Nis2RiskMeasure save(Nis2RiskMeasure measure) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(measure.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        Nis2RiskMeasureJpaEntity existing = measure.getId() != null
                ? jpa.findByIdAndTenantId(measure.getId(), currentTenant).orElse(null)
                : null;
        Nis2RiskMeasureJpaEntity saved = jpa.save(
                Nis2RiskMeasureMapper.toEntity(measure, existing));
        Nis2RiskMeasure out = Nis2RiskMeasureMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<Nis2RiskMeasure> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(Nis2RiskMeasureMapper::toDomain);
    }

    @Override
    public List<Nis2RiskMeasure> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(Nis2RiskMeasureMapper::toDomain).getContent();
    }

    @Override
    public List<Nis2RiskMeasure> findByTenantAndStatus(UUID tenantId, Nis2MeasureStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(Nis2RiskMeasureMapper::toDomain).getContent();
    }

    @Override
    public List<Nis2RiskMeasure> findByTenantAndCategory(UUID tenantId, Nis2MeasureCategory category) {
        return jpa.findByTenantIdAndCategory(tenantId, category,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(Nis2RiskMeasureMapper::toDomain).getContent();
    }

    @Override
    public Optional<Nis2RiskMeasure> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference)
                .map(Nis2RiskMeasureMapper::toDomain);
    }

    @Override
    public boolean existsByTenantAndReference(UUID tenantId, String reference) {
        return jpa.existsByTenantIdAndReference(tenantId, reference);
    }

    @Override
    public List<Nis2RiskMeasure> findReviewOverdue(Instant now, int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        return jpa.findReviewOverdue(Nis2MeasureStatus.DEPRECATED, now,
                        PageRequest.of(0, capped))
                .stream().map(Nis2RiskMeasureMapper::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        jpa.findByIdAndTenantId(id, tenant).ifPresent(jpa::delete);
    }
}
