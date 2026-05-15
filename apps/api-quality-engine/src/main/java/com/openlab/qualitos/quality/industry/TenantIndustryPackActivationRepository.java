package com.openlab.qualitos.quality.industry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantIndustryPackActivationRepository
        extends JpaRepository<TenantIndustryPackActivation, UUID> {

    Optional<TenantIndustryPackActivation> findByTenantIdAndPackCodeAndStatus(
            UUID tenantId, String packCode, ActivationStatus status);

    List<TenantIndustryPackActivation> findByTenantIdAndStatus(UUID tenantId, ActivationStatus status);

    List<TenantIndustryPackActivation> findByTenantIdOrderByActivatedAtDesc(UUID tenantId);
}
