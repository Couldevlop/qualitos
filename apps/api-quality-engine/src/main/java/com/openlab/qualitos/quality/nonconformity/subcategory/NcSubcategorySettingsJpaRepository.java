package com.openlab.qualitos.quality.nonconformity.subcategory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NcSubcategorySettingsJpaRepository extends JpaRepository<NcSubcategorySettingsJpaEntity, UUID> {

    Optional<NcSubcategorySettingsJpaEntity> findByTenantId(UUID tenantId);
}
