package com.openlab.qualitos.quality.standards;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequirementEvidenceRepository extends JpaRepository<RequirementEvidence, UUID> {

    Optional<RequirementEvidence> findByIdAndTenantStandardId(UUID id, UUID tenantStandardId);

    List<RequirementEvidence> findByTenantStandardId(UUID tenantStandardId);

    boolean existsByTenantStandardIdAndRequirementIdAndEvidenceRefId(
            UUID tenantStandardId, UUID requirementId, UUID evidenceRefId);
}
