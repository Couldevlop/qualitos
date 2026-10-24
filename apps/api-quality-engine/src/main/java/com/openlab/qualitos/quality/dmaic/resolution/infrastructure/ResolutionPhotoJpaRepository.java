package com.openlab.qualitos.quality.dmaic.resolution.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResolutionPhotoJpaRepository extends JpaRepository<ResolutionPhotoJpaEntity, UUID> {

    List<ResolutionPhotoJpaEntity> findByTenantIdAndProjectIdOrderBySlotAsc(UUID tenantId, UUID projectId);

    Optional<ResolutionPhotoJpaEntity> findByTenantIdAndProjectIdAndSlot(UUID tenantId, UUID projectId, int slot);

    boolean existsByObjectKey(String objectKey);
}
