package com.openlab.qualitos.quality.aiincidents.infrastructure;

import com.openlab.qualitos.quality.aiincidents.application.TenantProvider;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncident;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentRepository;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentSeverity;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AiIncidentRepositoryAdapter implements AiIncidentRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final AiIncidentJpaRepository jpa;
    private final TenantProvider tenantProvider;

    public AiIncidentRepositoryAdapter(
            AiIncidentJpaRepository jpa,
            @Qualifier("aiiTenantContextProvider") TenantProvider tenantProvider) {
        this.jpa = jpa;
        this.tenantProvider = tenantProvider;
    }

    @Override
    public AiIncident save(AiIncident incident) {
        UUID currentTenant = tenantProvider.requireTenantId();
        if (!currentTenant.equals(incident.getTenantId())) {
            throw new IllegalStateException("Cross-tenant save attempt");
        }
        AiIncidentJpaEntity existing = incident.getId() != null
                ? jpa.findByIdAndTenantId(incident.getId(), currentTenant).orElse(null)
                : null;
        AiIncidentJpaEntity saved = jpa.save(AiIncidentMapper.toEntity(incident, existing));
        AiIncident out = AiIncidentMapper.toDomain(saved);
        out.assignId(saved.getId());
        return out;
    }

    @Override
    public Optional<AiIncident> findById(UUID id) {
        UUID tenant = tenantProvider.requireTenantId();
        return jpa.findByIdAndTenantId(id, tenant).map(AiIncidentMapper::toDomain);
    }

    @Override
    public List<AiIncident> findByTenant(UUID tenantId) {
        return jpa.findByTenantId(tenantId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(AiIncidentMapper::toDomain).getContent();
    }

    @Override
    public List<AiIncident> findByTenantAndStatus(UUID tenantId, AiIncidentStatus status) {
        return jpa.findByTenantIdAndStatus(tenantId, status,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(AiIncidentMapper::toDomain).getContent();
    }

    @Override
    public List<AiIncident> findByTenantAndAiSystemId(UUID tenantId, UUID aiSystemId) {
        return jpa.findByTenantIdAndAiSystemId(tenantId, aiSystemId,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(AiIncidentMapper::toDomain).getContent();
    }

    @Override
    public List<AiIncident> findByTenantAndSeverity(UUID tenantId, AiIncidentSeverity sev) {
        return jpa.findByTenantIdAndSeverity(tenantId, sev,
                        PageRequest.of(0, MAX_PAGE_SIZE, Sort.by("createdAt").descending()))
                .map(AiIncidentMapper::toDomain).getContent();
    }

    @Override
    public List<AiIncident> findOverdueForRegulatorNotification(
            UUID tenantId, Instant now, int limit) {
        return jpa.findOverdueForRegulatorNotification(tenantId, now, limit).stream()
                .map(AiIncidentMapper::toDomain).toList();
    }

    @Override
    public Optional<AiIncident> findByTenantAndReference(UUID tenantId, String reference) {
        return jpa.findByTenantIdAndReference(tenantId, reference)
                .map(AiIncidentMapper::toDomain);
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
