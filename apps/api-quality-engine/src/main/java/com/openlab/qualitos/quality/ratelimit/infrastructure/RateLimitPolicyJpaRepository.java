package com.openlab.qualitos.quality.ratelimit.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RateLimitPolicyJpaRepository extends JpaRepository<RateLimitPolicyJpaEntity, UUID> {

    Optional<RateLimitPolicyJpaEntity> findByTenantIdAndScopeAndEnabledTrue(UUID tenantId, String scope);

    Optional<RateLimitPolicyJpaEntity> findByTenantIdAndScope(UUID tenantId, String scope);

    List<RateLimitPolicyJpaEntity> findByTenantIdOrderByScopeAsc(UUID tenantId);
}
