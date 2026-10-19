package com.openlab.qualitos.quality.docs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentFolderRepository extends JpaRepository<DocumentFolder, UUID> {

    List<DocumentFolder> findByTenantIdOrderByPositionAscNameAsc(UUID tenantId);

    Optional<DocumentFolder> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndParentId(UUID tenantId, UUID parentId);
}
