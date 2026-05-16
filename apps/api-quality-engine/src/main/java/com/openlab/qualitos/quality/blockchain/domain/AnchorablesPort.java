package com.openlab.qualitos.quality.blockchain.domain;

import java.util.List;
import java.util.UUID;

/**
 * Port côté audit log : charge les événements à ancrer, marque les ancrés.
 * L'adapter en infrastructure pioche dans le AuditEventRepository.
 */
public interface AnchorablesPort {

    List<Anchorable> loadUnanchored(UUID tenantId, int limit);

    /** Marque chaque événement avec le même txRef (un batch = une tx blockchain). */
    int markAnchored(UUID tenantId, List<UUID> eventIds, String txRef);
}
