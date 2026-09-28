package com.openlab.qualitos.quality.costofquality.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoqLabelJpaRepository extends JpaRepository<CoqLabelJpaEntity, UUID> {

    /** Le catalogue livré (sans tenant) et les libellés de CE client — jamais ceux d'un autre. */
    @Query("select l from CoqLabelJpaEntity l where l.tenantId is null or l.tenantId = :tenant")
    List<CoqLabelJpaEntity> findVisible(@Param("tenant") UUID tenant);

    @Query("select l from CoqLabelJpaEntity l "
            + "where l.id = :id and (l.tenantId is null or l.tenantId = :tenant)")
    Optional<CoqLabelJpaEntity> findVisibleById(@Param("id") UUID id, @Param("tenant") UUID tenant);
}
