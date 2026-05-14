package com.openlab.qualitos.quality.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuditChecklistItemRepository extends JpaRepository<AuditChecklistItem, UUID> {

    Optional<AuditChecklistItem> findByIdAndPlanId(UUID id, UUID planId);
}
