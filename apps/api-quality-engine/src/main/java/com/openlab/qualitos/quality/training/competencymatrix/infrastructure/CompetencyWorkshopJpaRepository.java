package com.openlab.qualitos.quality.training.competencymatrix.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CompetencyWorkshopJpaRepository extends JpaRepository<CompetencyWorkshopJpaEntity, UUID> {

    List<CompetencyWorkshopJpaEntity> findByTenantIdOrderByPositionAsc(UUID tenantId);

    /**
     * Supprime la liste du client en une requête, exécutée tout de suite : la
     * nouvelle liste peut reprendre les mêmes noms sans heurter l'unicité.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CompetencyWorkshopJpaEntity w where w.tenantId = :tenantId")
    void deleteByTenant(@Param("tenantId") UUID tenantId);
}
