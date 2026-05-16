package com.openlab.qualitos.quality.dpoappointments.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DpoAppointmentRepository {

    DpoAppointment save(DpoAppointment appointment);

    Optional<DpoAppointment> findById(UUID id);

    List<DpoAppointment> findByTenant(UUID tenantId);

    List<DpoAppointment> findByTenantAndStatus(UUID tenantId, DpoAppointmentStatus status);

    /** Désignation ACTIVE pour ce scope (au plus une à un instant donné). */
    Optional<DpoAppointment> findActiveByScope(UUID tenantId, String scope);

    Optional<DpoAppointment> findByTenantAndReference(UUID tenantId, String reference);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    void delete(UUID id);
}
