package com.openlab.qualitos.core.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour TenantModule.
 * Le filtre Hibernate tenantFilter doit être activé sur la session
 * avant tout appel à ce repository (voir TenantHibernateFilterInterceptor).
 */
@Repository
public interface TenantModuleRepository extends JpaRepository<TenantModule, UUID> {

    List<TenantModule> findByTenantIdAndActiveTrue(UUID tenantId);

    Optional<TenantModule> findByTenantIdAndModuleName(UUID tenantId, String moduleName);

    boolean existsByTenantIdAndModuleNameAndActiveTrue(UUID tenantId, String moduleName);
}
