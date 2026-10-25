package com.openlab.qualitos.quality.airouting.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantAiPolicyJpaRepository extends JpaRepository<TenantAiPolicyJpaEntity, UUID> {
}
