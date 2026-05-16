package com.openlab.qualitos.quality.ropa.infrastructure;

import com.openlab.qualitos.quality.ropa.application.TenantProvider;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivity;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityRepository;
import com.openlab.qualitos.quality.ropa.domain.ProcessingActivityStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProcessingActivityRepositoryAdapter implements ProcessingActivityRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final ProcessingActivityJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public ProcessingActivityRepositoryAdapter(
            ProcessingActivityJpaRepository jpa,
            @Qualifier("ropaTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public ProcessingActivity save(ProcessingActivity activity) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(activity.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        ProcessingActivityJpaEntity existing = activity.getId() != null
                ? jpa.findByIdAndTenantId(activity.getId(), currentTenant).orElse(null)
                : null;
        ProcessingActivityJpaEntity saved = jpa.save(
                ProcessingActivityMapper.toEntity(activity, existing));
        ProcessingActivity out = ProcessingActivityMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<ProcessingActivity> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(ProcessingActivityMapper::toDomain);
    }

    @Override
    public List<ProcessingActivity> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(ProcessingActivityMapper::toDomain).getContent();
    }

    @Override
    public List<ProcessingActivity> findByTenantAndStatus(UUID tenantId,
                                                          ProcessingActivityStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(ProcessingActivityMapper::toDomain).getContent();
    }

    @Override
    public Optional<ProcessingActivity> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference)
                .map(ProcessingActivityMapper::toDomain);
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
