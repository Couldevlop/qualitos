package com.openlab.qualitos.quality.aiincidents.infrastructure;

import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentSeverity;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiIncidentJpaRepository extends JpaRepository<AiIncidentJpaEntity, UUID> {

    Optional<AiIncidentJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<AiIncidentJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<AiIncidentJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, AiIncidentStatus status, Pageable pageable);

    Page<AiIncidentJpaEntity> findByTenantIdAndAiSystemId(
            UUID tenantId, UUID aiSystemId, Pageable pageable);

    Page<AiIncidentJpaEntity> findByTenantIdAndSeverity(
            UUID tenantId, AiIncidentSeverity severity, Pageable pageable);

    Optional<AiIncidentJpaEntity> findByTenantIdAndReference(UUID tenantId, String reference);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);

    /**
     * Incidents non notifiés au régulateur avec échéance dépassée.
     * NOTE : on filtre côté DB sur (status, deadline) ; la deadline est dérivée
     *        de detected_at + délai par sévérité (calcul dans la requête).
     *        Index partiel {@code idx_aii_pending_notification} sert le scan.
     */
    @Query(value = """
            SELECT * FROM ai_act_incidents
             WHERE tenant_id = :tenantId
               AND status IN ('DETECTED','INVESTIGATING')
               AND (detected_at +
                    CASE severity
                        WHEN 'DEATH_OR_SERIOUS_HARM_TO_HEALTH'        THEN INTERVAL '2 days'
                        WHEN 'SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS' THEN INTERVAL '10 days'
                        WHEN 'CRITICAL_INFRASTRUCTURE_DISRUPTION'      THEN INTERVAL '15 days'
                        WHEN 'SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE' THEN INTERVAL '15 days'
                    END) < :now
             ORDER BY detected_at ASC
             LIMIT :limit
            """, nativeQuery = true)
    List<AiIncidentJpaEntity> findOverdueForRegulatorNotification(
            @Param("tenantId") UUID tenantId,
            @Param("now") Instant now,
            @Param("limit") int limit);
}
