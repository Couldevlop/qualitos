package com.openlab.qualitos.core.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** Les membres actifs d'un client : ce que compte le plafond de la licence (ADR 0082). */
    long countByTenantIdAndActiveTrue(UUID tenantId);

    /**
     * Annuaire (ADR 0091) : membres ACTIFS du client dont la clé de recherche
     * contient le motif, triés par nom affiché. Le client est un paramètre
     * explicite, en plus du filtre Hibernate et de la RLS : une requête
     * d'annuaire sans client ne doit pas pouvoir s'écrire.
     *
     * @param pattern motif LIKE déjà échappé avec {@code !} (voir {@code SearchText})
     */
    @Query(value = """
            select new com.openlab.qualitos.core.user.DirectoryRow(u.id, u.keycloakId, u.displayName, u.email, u.active)
            from AppUser u
            where u.tenantId = :tenantId and u.active = true
              and u.searchKey like :pattern escape '!'
            order by lower(coalesce(u.displayName, u.email)) asc, u.id asc
            """,
            countQuery = """
            select count(u) from AppUser u
            where u.tenantId = :tenantId and u.active = true
              and u.searchKey like :pattern escape '!'
            """)
    Page<DirectoryRow> searchDirectory(@Param("tenantId") UUID tenantId,
                                       @Param("pattern") String pattern,
                                       Pageable pageable);

    /** Un membre du client, actif ou non, pour afficher le nom d'un responsable passé. */
    @Query("""
            select new com.openlab.qualitos.core.user.DirectoryRow(u.id, u.keycloakId, u.displayName, u.email, u.active)
            from AppUser u
            where u.id = :id and u.tenantId = :tenantId
            """)
    Optional<DirectoryRow> findDirectoryRow(@Param("tenantId") UUID tenantId, @Param("id") UUID id);

    /**
     * Un membre du client par son compte de connexion ({@code sub} du jeton) :
     * l'identifiant que portent tous les enregistrements qualité (pilote CAPA,
     * auditeur responsable, décideur d'un changement).
     */
    @Query("""
            select new com.openlab.qualitos.core.user.DirectoryRow(u.id, u.keycloakId, u.displayName, u.email, u.active)
            from AppUser u
            where u.keycloakId = :accountId and u.tenantId = :tenantId
            """)
    Optional<DirectoryRow> findDirectoryRowByAccount(@Param("tenantId") UUID tenantId,
                                                     @Param("accountId") String accountId);
}
