package com.openlab.qualitos.quality.ehs.infrastructure;

import com.openlab.qualitos.quality.ehs.domain.Incident;
import com.openlab.qualitos.quality.ehs.domain.IncidentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter Spring/JPA — implémente le port {@link IncidentRepository}.
 * C'est le SEUL endroit où l'agrégat parle JPA. La couche application est isolée.
 */
@Component
public class IncidentRepositoryAdapter implements IncidentRepository {

    private final IncidentJpaRepository jpa;

    public IncidentRepositoryAdapter(IncidentJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public Incident save(Incident incident) {
        IncidentJpaEntity existing = incident.getId() != null
                ? jpa.findById(incident.getId()).orElse(null)
                : null;
        IncidentJpaEntity entity = IncidentMapper.toEntity(incident, existing);
        IncidentJpaEntity saved = jpa.save(entity);
        if (incident.getId() == null) incident.assignId(saved.getId());
        return IncidentMapper.toDomain(saved);
    }

    @Override
    public Optional<Incident> findById(UUID id) {
        return jpa.findById(id).map(IncidentMapper::toDomain);
    }

    @Override
    public Optional<Incident> findByTenantIdAndCode(UUID tenantId, String code) {
        return jpa.findByTenantIdAndCode(tenantId, code).map(IncidentMapper::toDomain);
    }

    @Override
    public PagedResult<Incident> list(UUID tenantId, IncidentFilter filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<IncidentJpaEntity> result;
        if (filter.status() != null) {
            result = jpa.findByTenantIdAndStatus(tenantId, filter.status(), pageable);
        } else if (filter.type() != null) {
            result = jpa.findByTenantIdAndType(tenantId, filter.type(), pageable);
        } else if (filter.severity() != null) {
            result = jpa.findByTenantIdAndSeverity(tenantId, filter.severity(), pageable);
        } else {
            result = jpa.findByTenantId(tenantId, pageable);
        }
        return new PagedResult<>(
                result.getContent().stream().map(IncidentMapper::toDomain).toList(),
                result.getTotalElements(), page, size);
    }

    @Override
    public long countByTenantIdAndStatus(UUID tenantId,
                                         com.openlab.qualitos.quality.ehs.domain.IncidentStatus status) {
        return jpa.countByTenantIdAndStatus(tenantId, status);
    }

    @Override
    public long countByTenantIdAndType(UUID tenantId,
                                       com.openlab.qualitos.quality.ehs.domain.IncidentType type) {
        return jpa.countByTenantIdAndType(tenantId, type);
    }

    @Override
    public void delete(Incident incident) {
        if (incident.getId() != null) jpa.deleteById(incident.getId());
    }
}
