package com.openlab.qualitos.core.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour AppUser.
 * Le filtre Hibernate tenantFilter doit être activé sur la session
 * avant tout appel à ce repository (voir TenantHibernateFilterInterceptor).
 */
@Repository
public interface UserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByKeycloakId(String keycloakId);

    Optional<AppUser> findByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByKeycloakId(String keycloakId);
}
