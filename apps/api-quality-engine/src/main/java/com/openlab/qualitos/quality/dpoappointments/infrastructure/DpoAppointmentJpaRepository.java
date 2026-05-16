package com.openlab.qualitos.quality.dpoappointments.infrastructure;

import com.openlab.qualitos.quality.dpoappointments.domain.DpoAppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DpoAppointmentJpaRepository
        extends JpaRepository<DpoAppointmentJpaEntity, UUID> {

    Optional<DpoAppointmentJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<DpoAppointmentJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<DpoAppointmentJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, DpoAppointmentStatus status, Pageable pageable);

    Optional<DpoAppointmentJpaEntity> findByTenantIdAndScopeAndStatus(
            UUID tenantId, String scope, DpoAppointmentStatus status);

    Optional<DpoAppointmentJpaEntity> findByTenantIdAndReference(
            UUID tenantId, String reference);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);
}
