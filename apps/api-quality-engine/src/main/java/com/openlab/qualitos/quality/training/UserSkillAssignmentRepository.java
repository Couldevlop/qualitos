package com.openlab.qualitos.quality.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSkillAssignmentRepository extends JpaRepository<UserSkillAssignment, UUID> {

    Optional<UserSkillAssignment> findByTenantIdAndUserIdAndSkillId(
            UUID tenantId, UUID userId, UUID skillId);

    List<UserSkillAssignment> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    /**
     * Toutes les evaluations d'un tenant : elles forment les cellules de la
     * matrice de competences, et decident de ses colonnes.
     */
    List<UserSkillAssignment> findByTenantId(UUID tenantId);

    long countByTenantIdAndSkillId(UUID tenantId, UUID skillId);
}
