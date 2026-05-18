package com.openlab.qualitos.quality.aieudb.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EudbRegistrationRepository {

    EudbRegistration save(EudbRegistration registration);

    Optional<EudbRegistration> findById(UUID id);

    List<EudbRegistration> findByTenant(UUID tenantId);

    List<EudbRegistration> findByTenantAndStatus(UUID tenantId, EudbRegistrationStatus status);

    List<EudbRegistration> findByTenantAndAiSystemId(UUID tenantId, UUID aiSystemId);

    Optional<EudbRegistration> findByTenantAndReference(UUID tenantId, String reference);

    Optional<EudbRegistration> findByTenantAndEudbId(UUID tenantId, String eudbId);

    boolean existsByTenantAndReference(UUID tenantId, String reference);

    void delete(UUID id);
}
