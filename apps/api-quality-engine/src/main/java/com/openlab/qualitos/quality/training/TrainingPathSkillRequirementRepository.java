package com.openlab.qualitos.quality.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingPathSkillRequirementRepository
        extends JpaRepository<TrainingPathSkillRequirement, UUID> {

    Optional<TrainingPathSkillRequirement> findByPathIdAndSkillId(UUID pathId, UUID skillId);

    List<TrainingPathSkillRequirement> findByPathId(UUID pathId);

    void deleteByPathId(UUID pathId);

    long countByPathId(UUID pathId);
}
