package com.openlab.qualitos.quality.fives;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FiveSAuditItemRepository extends JpaRepository<FiveSAuditItem, UUID> {

    Optional<FiveSAuditItem> findByAuditIdAndPillar(UUID auditId, FiveSPillar pillar);
}
