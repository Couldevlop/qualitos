package com.openlab.qualitos.quality.cyberincidents.infrastructure;

import com.openlab.qualitos.quality.cyberincidents.application.TenantProvider;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncident;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentRepository;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CyberIncidentRepositoryAdapter implements CyberIncidentRepository {

    private static final int MAX_PAGE_SIZE = 500;
    private static final List<CyberIncidentStatus> TERMINALS =
            List.of(CyberIncidentStatus.CLOSED, CyberIncidentStatus.REJECTED);

    private final CyberIncidentJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public CyberIncidentRepositoryAdapter(
            CyberIncidentJpaRepository jpa,
            @Qualifier("cybTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public CyberIncident save(CyberIncident incident) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(incident.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        CyberIncidentJpaEntity existing = incident.getId() != null
                ? jpa.findByIdAndTenantId(incident.getId(), currentTenant).orElse(null)
                : null;
        CyberIncidentJpaEntity saved = jpa.save(CyberIncidentMapper.toEntity(incident, existing));
        CyberIncident out = CyberIncidentMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<CyberIncident> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(CyberIncidentMapper::toDomain);
    }

    @Override
    public List<CyberIncident> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("detectedAt").descending()))
                .map(CyberIncidentMapper::toDomain).getContent();
    }

    @Override
    public List<CyberIncident> findByTenantAndStatus(UUID tenantId, CyberIncidentStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("detectedAt").descending()))
                .map(CyberIncidentMapper::toDomain).getContent();
    }

    @Override
    public boolean existsByTenantAndReference(UUID tenantId, String reference) {
        return jpa.existsByTenantIdAndReference(tenantId, reference);
    }

    @Override
    public Optional<CyberIncident> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference)
                .map(CyberIncidentMapper::toDomain);
    }

    @Override
    public List<CyberIncident> findEarlyWarningOverdue(Instant now, int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        return jpa.findEarlyWarningOverdue(TERMINALS, now, PageRequest.of(0, capped))
                .stream().map(CyberIncidentMapper::toDomain).toList();
    }

    @Override
    public List<CyberIncident> findInitialAssessmentOverdue(Instant now, int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        return jpa.findInitialAssessmentOverdue(TERMINALS, now, PageRequest.of(0, capped))
                .stream().map(CyberIncidentMapper::toDomain).toList();
    }

    @Override
    public List<CyberIncident> findFinalReportOverdue(Instant now, int limit) {
        int capped = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        return jpa.findFinalReportOverdue(TERMINALS, now, PageRequest.of(0, capped))
                .stream().map(CyberIncidentMapper::toDomain).toList();
    }

    @Override
    public void delete(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        jpa.findByIdAndTenantId(id, tenant).ifPresent(jpa::delete);
    }
}
