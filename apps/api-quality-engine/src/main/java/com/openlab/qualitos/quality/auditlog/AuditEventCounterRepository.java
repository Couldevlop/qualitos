package com.openlab.qualitos.quality.auditlog;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface AuditEventCounterRepository extends JpaRepository<AuditEventCounter, UUID> {

    /**
     * Charge le compteur d'un tenant avec verrou pessimiste — les inserts
     * concurrents sur le même tenant sont sérialisés.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuditEventCounter> findById(UUID tenantId);
}
