package com.openlab.qualitos.quality.standards;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StandardRequirementRepository extends JpaRepository<StandardRequirement, UUID> {

    List<StandardRequirement> findByClauseSectionStandardId(UUID standardId);
}
