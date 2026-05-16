package com.openlab.qualitos.quality.crossbordertransfers.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrossBorderTransferRepository {

    CrossBorderTransfer save(CrossBorderTransfer transfer);

    Optional<CrossBorderTransfer> findById(UUID id);

    List<CrossBorderTransfer> findByTenant(UUID tenantId);

    List<CrossBorderTransfer> findByTenantAndStatus(UUID tenantId, CrossBorderTransferStatus status);

    Optional<CrossBorderTransfer> findByTenantAndReference(UUID tenantId, String reference);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    void delete(UUID id);
}
