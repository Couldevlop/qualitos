package com.openlab.qualitos.quality.ropa.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessingActivityRepository {

    ProcessingActivity save(ProcessingActivity activity);

    Optional<ProcessingActivity> findById(UUID id);

    List<ProcessingActivity> findByTenant(UUID tenantId);

    List<ProcessingActivity> findByTenantAndStatus(UUID tenantId, ProcessingActivityStatus status);

    Optional<ProcessingActivity> findByTenantAndReference(UUID tenantId, String reference);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    void delete(UUID id);
}
