package com.openlab.qualitos.quality.fivewhys;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Toutes les signatures portent le tenant : aucune lecture ne peut l'oublier. */
public interface FiveWhysAnalysisRepository extends JpaRepository<FiveWhysAnalysis, UUID> {

    Optional<FiveWhysAnalysis> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<FiveWhysAnalysis> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    List<FiveWhysAnalysis> findByNonConformityIdAndTenantIdOrderByCreatedAtDesc(
            UUID ncId, UUID tenantId);
}
