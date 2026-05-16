package com.openlab.qualitos.quality.crossbordertransfers.infrastructure;

import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CrossBorderTransferJpaRepository
        extends JpaRepository<CrossBorderTransferJpaEntity, UUID> {

    Optional<CrossBorderTransferJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<CrossBorderTransferJpaEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<CrossBorderTransferJpaEntity> findByTenantIdAndStatus(
            UUID tenantId, CrossBorderTransferStatus status, Pageable pageable);

    Optional<CrossBorderTransferJpaEntity> findByTenantIdAndReference(
            UUID tenantId, String reference);

    boolean existsByTenantIdAndReference(UUID tenantId, String reference);
}
