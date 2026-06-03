package com.openlab.qualitos.quality.activityfeed;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditActivityRepository extends JpaRepository<AuditActivityEntry, UUID> {

    /** Idempotence du consommateur (at-least-once) : ne pas réinsérer un événement déjà projeté. */
    boolean existsByTenantIdAndSequenceNo(UUID tenantId, long sequenceNo);

    /** Flux d'activité récent d'un tenant (vues dashboard). */
    Page<AuditActivityEntry> findByTenantIdOrderBySequenceNoDesc(UUID tenantId, Pageable pageable);
}
