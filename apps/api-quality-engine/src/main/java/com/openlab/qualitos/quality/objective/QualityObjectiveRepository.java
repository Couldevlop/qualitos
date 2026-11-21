package com.openlab.qualitos.quality.objective;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QualityObjectiveRepository extends JpaRepository<QualityObjective, UUID> {

    /**
     * Un objectif de CE client. Le tenant est dans la signature, et pas seulement
     * dans le filtre Hibernate : une lecture par identifiant seul laisserait un
     * UUID deviné traverser la frontière entre clients (règle 18.2 #2).
     */
    Optional<QualityObjective> findByIdAndTenantId(UUID id, UUID tenantId);

    /** La revue : les objectifs d'un client, du plus récent au plus ancien. */
    List<QualityObjective> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<QualityObjective> findByTenantIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, ObjectiveLifecycle status);

    /** Les objectifs d'un client qui suivent ce KPI. */
    List<QualityObjective> findByTenantIdAndKpiIdAndStatus(
            UUID tenantId, UUID kpiId, ObjectiveLifecycle status);

    /** Un KPI encore visé par un objectif ne se supprime pas sans qu'on le sache. */
    long countByTenantIdAndKpiId(UUID tenantId, UUID kpiId);
}
