package com.openlab.qualitos.quality.standards;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Accès au catalogue normatif.
 *
 * <p>AUCUNE méthode de lecture sans tenant, y compris celles qu'offre
 * {@code JpaRepository} : le catalogue mêle des normes de plateforme et des
 * référentiels appartenant à des tenants, et une seule lecture non filtrée
 * exposerait les procédures internes d'une organisation à toutes les autres.
 * {@code StandardTenantIsolationTest} interdit la réapparition d'une telle
 * méthode.
 */
public interface StandardRepository extends JpaRepository<Standard, UUID> {

    String VISIBLE = "(s.ownerTenantId is null or s.ownerTenantId = :tenantId)";

    @Query("select s from Standard s where " + VISIBLE)
    Page<Standard> findVisible(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("select s from Standard s where s.status = :status and " + VISIBLE)
    Page<Standard> findVisibleByStatus(@Param("status") StandardStatus status,
                                       @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("select s from Standard s where s.family = :family and " + VISIBLE)
    Page<Standard> findVisibleByFamily(@Param("family") String family,
                                       @Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("select s from Standard s where s.id = :id and " + VISIBLE)
    Optional<Standard> findVisibleById(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("select s from Standard s where s.code = :code and " + VISIBLE)
    Optional<Standard> findVisibleByCode(@Param("code") String code, @Param("tenantId") UUID tenantId);

    /** Pour l'ÉCRITURE : seul un référentiel appartenant au tenant est modifiable. */
    @Query("select s from Standard s where s.id = :id and s.ownerTenantId = :tenantId")
    Optional<Standard> findOwnedById(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("select count(s) > 0 from Standard s where s.sourceDocumentId = :documentId")
    boolean existsBySourceDocument(@Param("documentId") UUID documentId);
}
