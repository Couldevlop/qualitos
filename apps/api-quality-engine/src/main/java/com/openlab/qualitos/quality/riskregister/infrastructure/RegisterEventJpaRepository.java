package com.openlab.qualitos.quality.riskregister.infrastructure;

import com.openlab.qualitos.quality.riskregister.domain.RegisterItemKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegisterEventJpaRepository extends JpaRepository<RegisterEventJpaEntity, UUID> {

    List<RegisterEventJpaEntity> findByTenantIdAndItemKindAndItemIdOrderByOccurredAtDesc(
            UUID tenantId, RegisterItemKind itemKind, UUID itemId);
}
