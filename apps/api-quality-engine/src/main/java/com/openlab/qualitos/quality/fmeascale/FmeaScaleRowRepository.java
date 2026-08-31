package com.openlab.qualitos.quality.fmeascale;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Les lignes de barème redéfinies par un tenant.
 *
 * <p>Toutes les méthodes portent le tenant : le filtrage n'est jamais laissé au
 * code appelant (§18.2 #2). Le tri décroissant est celui du référentiel — le
 * plus grave en tête.
 */
public interface FmeaScaleRowRepository extends JpaRepository<FmeaScaleRowEntity, UUID> {

    List<FmeaScaleRowEntity> findByTenantIdAndKindOrderByScoreDesc(UUID tenantId, String kind);

    List<FmeaScaleRowEntity> findByTenantIdOrderByScoreDesc(UUID tenantId);

    void deleteByTenantIdAndKind(UUID tenantId, String kind);
}
