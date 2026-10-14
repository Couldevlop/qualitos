package com.openlab.qualitos.quality.change;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChangeHistoryRepository extends JpaRepository<ChangeHistoryEntry, UUID> {

    /** Le plus récent en haut, comme l'écran l'affiche. */
    List<ChangeHistoryEntry> findByTenantIdAndChangeIdOrderByOccurredAtDesc(UUID tenantId, UUID changeId);

    void deleteByTenantIdAndChangeId(UUID tenantId, UUID changeId);
}
