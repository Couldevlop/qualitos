package com.openlab.qualitos.quality.authz.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Les rôles réglés ou créés par un client. Toutes les requêtes portent le client. */
public interface AuthzRoleRepository extends JpaRepository<AuthzRoleJpaEntity, UUID> {

    List<AuthzRoleJpaEntity> findByTenantId(UUID tenantId);

    Optional<AuthzRoleJpaEntity> findByTenantIdAndCode(UUID tenantId, String code);
}
