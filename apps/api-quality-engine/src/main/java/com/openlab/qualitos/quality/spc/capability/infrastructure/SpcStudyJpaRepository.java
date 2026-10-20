package com.openlab.qualitos.quality.spc.capability.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpcStudyJpaRepository extends JpaRepository<SpcStudyJpaEntity, UUID> {

    Optional<SpcStudyJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<SpcStudyJpaEntity> findByTenantId(UUID tenantId);
}
