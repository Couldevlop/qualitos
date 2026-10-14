package com.openlab.qualitos.quality.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuditReportRepository extends JpaRepository<AuditReport, UUID> {

    Optional<AuditReport> findByTenantIdAndPlanId(UUID tenantId, UUID planId);

    /**
     * Le plan a déjà été relu pour le tenant courant par l'appelant : la question
     * ne porte plus que sur l'état de SON rapport.
     */
    boolean existsByPlanIdAndStatus(UUID planId, AuditReportStatus status);
}
