package com.openlab.qualitos.quality.fivewhys;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FiveWhysStepRepository extends JpaRepository<FiveWhysStep, UUID> {

    Optional<FiveWhysStep> findByIdAndTenantId(UUID id, UUID tenantId);

    /** L'ordre EST le sens de la méthode : jamais de lecture non ordonnée. */
    List<FiveWhysStep> findByAnalysisIdAndTenantIdOrderByPositionAsc(UUID analysisId, UUID tenantId);

    /**
     * Les étapes de PLUSIEURS analyses, en une requête.
     *
     * <p>La fiche d'écart dessine la cascade de chacune de ses analyses : une
     * requête par analyse ferait autant d'allers-retours que d'analyses sur
     * l'écran le plus consulté du dossier.
     */
    List<FiveWhysStep> findByAnalysisIdInAndTenantIdOrderByAnalysisIdAscPositionAsc(
            Collection<UUID> analysisIds, UUID tenantId);

    long countByAnalysisIdAndTenantId(UUID analysisId, UUID tenantId);
}
