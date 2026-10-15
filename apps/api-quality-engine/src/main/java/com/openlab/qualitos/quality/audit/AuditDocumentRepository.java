package com.openlab.qualitos.quality.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditDocumentRepository extends JpaRepository<AuditDocument, UUID> {

    List<AuditDocument> findByTenantIdAndPlanIdOrderByCreatedAtAsc(UUID tenantId, UUID planId);

    Optional<AuditDocument> findByIdAndTenantIdAndPlanId(UUID id, UUID tenantId, UUID planId);

    long countByTenantIdAndPlanId(UUID tenantId, UUID planId);

    /** Poids déjà joint à l'audit, calculé en base plutôt que sur une liste chargée. */
    @Query("select coalesce(sum(d.sizeBytes), 0) from AuditDocument d "
            + "where d.tenantId = :tenantId and d.planId = :planId")
    long sumSizeBytes(UUID tenantId, UUID planId);

    /**
     * Existence par clé d'objet, sans filtre de tenant — pour le balayage des
     * orphelins, qui s'exécute hors requête. La clé porte déjà le tenant.
     */
    boolean existsByObjectKey(String objectKey);
}
