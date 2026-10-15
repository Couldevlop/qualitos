package com.openlab.qualitos.quality.change;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MocRoleHolderRepository extends JpaRepository<MocRoleHolderRow, UUID> {

    List<MocRoleHolderRow> findByTenantIdOrderByOrdinalAsc(UUID tenantId);
}
