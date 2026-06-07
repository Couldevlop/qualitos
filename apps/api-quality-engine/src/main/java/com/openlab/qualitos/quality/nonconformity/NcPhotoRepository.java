package com.openlab.qualitos.quality.nonconformity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NcPhotoRepository extends JpaRepository<NcPhoto, UUID> {

    List<NcPhoto> findByTenantIdAndNcIdOrderByCreatedAtAsc(UUID tenantId, UUID ncId);

    Optional<NcPhoto> findByIdAndTenantIdAndNcId(UUID id, UUID tenantId, UUID ncId);
}
