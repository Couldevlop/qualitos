package com.openlab.qualitos.quality.spc.gagerr.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GageRrStudyJpaRepository extends JpaRepository<GageRrStudyJpaEntity, UUID> {

    Optional<GageRrStudyJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<GageRrStudyJpaEntity> findByTenantId(UUID tenantId);
}
