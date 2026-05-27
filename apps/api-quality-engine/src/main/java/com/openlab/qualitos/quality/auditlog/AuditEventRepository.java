package com.openlab.qualitos.quality.auditlog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    /** Événements non encore ancrés sur blockchain, ordonnés par séquence. */
    List<AuditEvent> findByTenantIdAndBlockchainTxRefIsNullOrderBySequenceNoAsc(
            UUID tenantId, org.springframework.data.domain.Pageable pageable);

    /** Vérification d'ancrage : retrouve l'événement par son hash d'intégrité. */
    Optional<AuditEvent> findByTenantIdAndIntegrityHash(UUID tenantId, String integrityHash);

    /** Le lot ancré sous une même référence de reçu, en ordre de séquence. */
    List<AuditEvent> findByTenantIdAndBlockchainTxRefOrderBySequenceNoAsc(
            UUID tenantId, String blockchainTxRef);

    /** Tenants ayant des événements non ancrés (pilotage du scheduler d'ancrage). */
    @Query("select distinct e.tenantId from AuditEvent e where e.blockchainTxRef is null")
    List<UUID> findDistinctTenantIdsWithUnanchoredEvents();
}
