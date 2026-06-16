package com.openlab.qualitos.quality.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LearnerProgressRepository extends JpaRepository<LearnerProgress, UUID> {

    Optional<LearnerProgress> findByTenantIdAndUserId(UUID tenantId, UUID userId);
}
