package com.openlab.qualitos.core.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Le client d'une installation on-premise, créé avec l'identifiant que fixe
     * sa licence (ADR 0082) : l'entité génère sinon le sien, et les comptes, qui
     * portent cet identifiant dans leur jeton, ne retrouveraient pas leur client.
     */
    @Modifying
    @Query(value = "INSERT INTO tenants (id, slug, name, plan, active, created_at, updated_at) "
            + "VALUES (:id, :slug, :name, :plan, TRUE, :now, :now)", nativeQuery = true)
    void insertWithId(@Param("id") UUID id, @Param("slug") String slug, @Param("name") String name,
                      @Param("plan") String plan, @Param("now") Instant now);
}
