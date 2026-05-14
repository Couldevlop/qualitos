package com.openlab.qualitos.quality.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuditFindingRepository extends JpaRepository<AuditFinding, UUID> {

    Optional<AuditFinding> findByIdAndPlanId(UUID id, UUID planId);
}
