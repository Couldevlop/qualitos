package com.openlab.qualitos.quality.ishikawa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Toutes les lectures portent le tenant : aucune signature ne permet d'aller
 * chercher une action sans le préciser (§18.2 #2).
 */
public interface IshikawaActionRepository extends JpaRepository<IshikawaAction, UUID> {

    Optional<IshikawaAction> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Ordre de décision : le plan se lit comme un journal, pas comme un classement. */
    List<IshikawaAction> findByDiagramIdAndTenantIdOrderByCreatedAtAsc(UUID diagramId, UUID tenantId);

    void deleteByDiagramIdAndTenantId(UUID diagramId, UUID tenantId);
}
