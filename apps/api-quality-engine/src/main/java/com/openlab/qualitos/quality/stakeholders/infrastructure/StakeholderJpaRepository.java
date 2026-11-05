package com.openlab.qualitos.quality.stakeholders.infrastructure;

import com.openlab.qualitos.quality.stakeholders.domain.StakeholderCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StakeholderJpaRepository extends JpaRepository<StakeholderJpaEntity, UUID> {

    Optional<StakeholderJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<StakeholderJpaEntity> findByTenantId(UUID tenantId);

    /** Le doublon se cherche sans tenir compte de la casse : « Client A » et « client a » sont le même. */
    List<StakeholderJpaEntity> findByTenantIdAndCategoryAndNameIgnoreCase(
            UUID tenantId, StakeholderCategory category, String name);
}
