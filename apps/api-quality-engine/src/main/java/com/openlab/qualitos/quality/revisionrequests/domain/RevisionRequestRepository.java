package com.openlab.qualitos.quality.revisionrequests.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de persistance des demandes de révision. Toutes les recherches prennent
 * leur tenant explicitement : l'écouteur d'événements travaille après commit, et
 * dépendre du contexte d'exécution à ce moment-là serait fragile.
 */
public interface RevisionRequestRepository {

    RevisionRequest save(RevisionRequest request);

    Optional<RevisionRequest> findById(UUID id);

    List<RevisionRequest> findPendingByProduct(UUID tenantId, UUID productId);

    Optional<RevisionRequest> findPendingForTarget(UUID tenantId, RevisionTargetType type, UUID targetId);

    List<RevisionRequest> findByTrigger(UUID tenantId, UUID triggerRefId);

    int countPendingByProduct(UUID tenantId, UUID productId);
}
