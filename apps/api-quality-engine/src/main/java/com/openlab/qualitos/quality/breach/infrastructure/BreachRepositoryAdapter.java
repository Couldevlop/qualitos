package com.openlab.qualitos.quality.breach.infrastructure;

import com.openlab.qualitos.quality.breach.application.TenantProvider;
import com.openlab.qualitos.quality.breach.domain.BreachIncident;
import com.openlab.qualitos.quality.breach.domain.BreachRepository;
import com.openlab.qualitos.quality.breach.domain.BreachStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BreachRepositoryAdapter implements BreachRepository {

    private static final int MAX_PAGE_SIZE = 500;
    private static final List<BreachStatus> TERMINALS =
            List.of(BreachStatus.CLOSED, BreachStatus.REJECTED);

    private final BreachJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public BreachRepositoryAdapter(BreachJpaRepository jpa,
                                   @Qualifier("breachTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public BreachIncident save(BreachIncident incident) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(incident.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        BreachJpaEntity existing = incident.getId() != null
                ? jpa.findByIdAndTenantId(incident.getId(), currentTenant).orElse(null)
                : null;
        BreachJpaEntity saved = jpa.save(BreachMapper.toEntity(incident, existing));
        BreachIncident out = BreachMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<BreachIncident> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(BreachMapper::toDomain);
    }

    @Override
    public List<BreachIncident> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("detectedAt").descending()))
                .map(BreachMapper::toDomain).getContent();
    }

    @Override
    public List<BreachIncident> findByTenantAndStatus(UUID tenantId, BreachStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("detectedAt").descending()))
                .map(BreachMapper::toDomain).getContent();
    }

    @Override
    public List<BreachIncident> findDpaOverdue(Instant now, int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        return jpa.findDpaOverdue(TERMINALS, now, PageRequest.of(0, capped))
                .stream().map(BreachMapper::toDomain).toList();
    }

    @Override
    public boolean existsByTenantAndReference(UUID tenantId, String internalReference) {
        return jpa.existsByTenantIdAndInternalReference(tenantId, internalReference);
    }
}
