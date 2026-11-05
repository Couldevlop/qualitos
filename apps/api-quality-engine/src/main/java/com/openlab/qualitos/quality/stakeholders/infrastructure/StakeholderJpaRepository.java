package com.openlab.qualitos.quality.stakeholders.infrastructure;

import com.openlab.qualitos.quality.stakeholders.domain.StakeholderCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StakeholderJpaRepository extends JpaRepository<StakeholderJpaEntity, UUID> {

    Optional<StakeholderJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<StakeholderJpaEntity> findByTenantId(UUID tenantId);

    /**
     * Les fiches portant une exigence rattachée à ce dossier CAPA.
     *
     * <p>{@code distinct} parce qu'une même fiche peut porter plusieurs
     * exigences du même dossier ; le filtre de tenant reste explicite comme
     * partout ailleurs (ADR 0001).
     */
    @Query("""
            select distinct s from StakeholderJpaEntity s
            join s.requirements r
            where s.tenantId = :tenantId and r.capaId = :capaId
            """)
    List<StakeholderJpaEntity> findByCapa(@Param("tenantId") UUID tenantId,
                                          @Param("capaId") UUID capaId);

    /** Le doublon se cherche sans tenir compte de la casse : « Client A » et « client a » sont le même. */
    List<StakeholderJpaEntity> findByTenantIdAndCategoryAndNameIgnoreCase(
            UUID tenantId, StakeholderCategory category, String name);
}
