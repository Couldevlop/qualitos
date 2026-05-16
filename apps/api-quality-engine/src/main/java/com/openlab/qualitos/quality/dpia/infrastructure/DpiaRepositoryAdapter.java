package com.openlab.qualitos.quality.dpia.infrastructure;

import com.openlab.qualitos.quality.dpia.application.TenantProvider;
import com.openlab.qualitos.quality.dpia.domain.Dpia;
import com.openlab.qualitos.quality.dpia.domain.DpiaRepository;
import com.openlab.qualitos.quality.dpia.domain.DpiaStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DpiaRepositoryAdapter implements DpiaRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final DpiaJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public DpiaRepositoryAdapter(DpiaJpaRepository jpa,
                                 @Qualifier("dpiaTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public Dpia save(Dpia dpia) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(dpia.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        DpiaJpaEntity existing = dpia.getId() != null
                ? jpa.findByIdAndTenantId(dpia.getId(), currentTenant).orElse(null)
                : null;
        DpiaJpaEntity saved = jpa.save(DpiaMapper.toEntity(dpia, existing));
        Dpia out = DpiaMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<Dpia> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(DpiaMapper::toDomain);
    }

    @Override
    public List<Dpia> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(DpiaMapper::toDomain).getContent();
    }

    @Override
    public List<Dpia> findByTenantAndStatus(UUID tenantId, DpiaStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(DpiaMapper::toDomain).getContent();
    }

    @Override
    public Optional<Dpia> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference).map(DpiaMapper::toDomain);
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
