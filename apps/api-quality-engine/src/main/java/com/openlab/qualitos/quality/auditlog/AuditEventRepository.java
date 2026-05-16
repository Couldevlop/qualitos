package com.openlab.qualitos.quality.auditlog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Optional<AuditEvent> findTopByTenantIdOrderBySequenceNoDesc(UUID tenantId);

    Page<AuditEvent> findByTenantIdOrderBySequenceNoDesc(UUID tenantId, Pageable pageable);

    Page<AuditEvent> findByTenantIdAndActionOrderBySequenceNoDesc(
            UUID tenantId, String action, Pageable pageable);

    Page<AuditEvent> findByTenantIdAndResourceTypeAndResourceIdOrderBySequenceNoDesc(
            UUID tenantId, String resourceType, UUID resourceId, Pageable pageable);

    Page<AuditEvent> findByTenantIdAndActorUserIdOrderBySequenceNoDesc(
            UUID tenantId, UUID actorUserId, Pageable pageable);

    Page<AuditEvent> findByTenantIdAndOccurredAtBetweenOrderBySequenceNoDesc(
            UUID tenantId, Instant from, Instant to, Pageable pageable);

    /** Vérification de chaîne : récupère [fromSeq, toSeq] inclus en ordre croissant. */
    List<AuditEvent> findByTenantIdAndSequenceNoBetweenOrderBySequenceNoAsc(
            UUID tenantId, long fromSeq, long toSeq);
}
