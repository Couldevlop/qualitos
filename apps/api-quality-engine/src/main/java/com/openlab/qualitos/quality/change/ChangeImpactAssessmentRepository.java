package com.openlab.qualitos.quality.change;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ChangeImpactAssessmentRepository extends JpaRepository<ChangeImpactAssessment, UUID> {

    List<ChangeImpactAssessment> findByTenantIdAndChangeId(UUID tenantId, UUID changeId);

    List<ChangeImpactAssessment> findByTenantIdAndChangeIdIn(UUID tenantId, Collection<UUID> changeIds);

    void deleteByTenantIdAndChangeId(UUID tenantId, UUID changeId);
}
