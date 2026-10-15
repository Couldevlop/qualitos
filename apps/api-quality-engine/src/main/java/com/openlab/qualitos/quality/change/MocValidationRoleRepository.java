package com.openlab.qualitos.quality.change;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MocValidationRoleRepository extends JpaRepository<MocValidationRoleRow, UUID> {

    List<MocValidationRoleRow> findByTenantIdOrderByOrdinalAsc(UUID tenantId);
}
