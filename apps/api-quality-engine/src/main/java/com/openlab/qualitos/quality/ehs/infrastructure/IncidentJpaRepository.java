package com.openlab.qualitos.quality.ehs.infrastructure;

import com.openlab.qualitos.quality.ehs.domain.IncidentSeverity;
import com.openlab.qualitos.quality.ehs.domain.IncidentStatus;
import com.openlab.qualitos.quality.ehs.domain.IncidentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IncidentJpaRepository extends JpaRepository<IncidentJpaEntity, UUID> {

    Optional<IncidentJpaEntity> findByTenantIdAndCode(UUID tenantId, String code);

    Page<IncidentJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<IncidentJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, IncidentStatus status, Pageable pageable);

    Page<IncidentJpaEntity> findByTenantIdAndType(
            UUID tenantId, IncidentType type, Pageable pageable);

    Page<IncidentJpaEntity> findByTenantIdAndSeverity(
            UUID tenantId, IncidentSeverity severity, Pageable pageable);

    long countByTenantIdAndStatus(UUID tenantId, IncidentStatus status);

    long countByTenantIdAndType(UUID tenantId, IncidentType type);
}
