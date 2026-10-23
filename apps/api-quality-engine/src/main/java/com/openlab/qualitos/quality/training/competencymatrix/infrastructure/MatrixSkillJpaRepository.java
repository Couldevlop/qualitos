package com.openlab.qualitos.quality.training.competencymatrix.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatrixSkillJpaRepository extends JpaRepository<MatrixSkillJpaEntity, UUID> {

    List<MatrixSkillJpaEntity> findByTenantId(UUID tenantId);

    Optional<MatrixSkillJpaEntity> findByTenantIdAndSkillId(UUID tenantId, UUID skillId);
}
