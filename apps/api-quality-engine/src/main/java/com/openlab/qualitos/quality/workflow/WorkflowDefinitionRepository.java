package com.openlab.qualitos.quality.workflow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {

    Page<WorkflowDefinition> findByTenantId(UUID tenantId, Pageable pageable);

    Page<WorkflowDefinition> findByTenantIdAndStatus(UUID tenantId, WorkflowStatus status, Pageable pageable);

    Optional<WorkflowDefinition> findByIdAndTenantId(UUID id, UUID tenantId);
}
