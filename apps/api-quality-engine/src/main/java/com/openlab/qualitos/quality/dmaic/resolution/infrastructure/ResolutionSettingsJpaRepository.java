package com.openlab.qualitos.quality.dmaic.resolution.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResolutionSettingsJpaRepository extends JpaRepository<ResolutionSettingsJpaEntity, UUID> {

    Optional<ResolutionSettingsJpaEntity> findByTenantId(UUID tenantId);

    void deleteByTenantId(UUID tenantId);
}
