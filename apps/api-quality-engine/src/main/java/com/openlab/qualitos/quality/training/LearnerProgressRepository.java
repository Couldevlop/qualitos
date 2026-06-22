package com.openlab.qualitos.quality.training;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LearnerProgressRepository extends JpaRepository<LearnerProgress, UUID> {

    Optional<LearnerProgress> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    /**
     * Classement (leaderboard) du tenant : apprenants triés par points décroissants.
     * Filtré strictement par {@code tenantId} (issu du JWT) — pas de fuite cross-tenant.
     */
    Page<LearnerProgress> findByTenantIdOrderByPointsDescCompletedCountDesc(UUID tenantId, Pageable pageable);
}
