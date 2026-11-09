package com.openlab.qualitos.quality.dmaic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * La bibliotheque de detrompeurs : celle de la plateforme, plus celle du client.
 *
 * <p>Toutes les lectures passent par les requetes {@code visible*} : un client
 * voit le catalogue partage ({@code tenant_id IS NULL}) et ses propres
 * dispositifs, jamais ceux d'un autre. Les methodes derivees sans filtre de
 * client ne sont pas exposees ici, pour qu'aucun appel ne puisse les prendre
 * par inadvertance.
 */
public interface PokaYokeDeviceRepository extends JpaRepository<PokaYokeDevice, UUID> {

    @Query("SELECT d FROM PokaYokeDevice d WHERE d.tenantId IS NULL OR d.tenantId = :tenantId")
    Page<PokaYokeDevice> findVisible(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT d FROM PokaYokeDevice d WHERE (d.tenantId IS NULL OR d.tenantId = :tenantId) "
            + "AND d.type = :type")
    Page<PokaYokeDevice> findVisibleByType(@Param("tenantId") UUID tenantId,
                                           @Param("type") PokaYokeType type, Pageable pageable);

    @Query("SELECT d FROM PokaYokeDevice d WHERE (d.tenantId IS NULL OR d.tenantId = :tenantId) "
            + "AND d.mechanism = :mechanism")
    Page<PokaYokeDevice> findVisibleByMechanism(@Param("tenantId") UUID tenantId,
                                                @Param("mechanism") PokaYokeMechanism mechanism,
                                                Pageable pageable);

    @Query("SELECT d FROM PokaYokeDevice d WHERE d.id = :id "
            + "AND (d.tenantId IS NULL OR d.tenantId = :tenantId)")
    Optional<PokaYokeDevice> findVisibleById(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT d FROM PokaYokeDevice d WHERE d.code = :code "
            + "AND (d.tenantId IS NULL OR d.tenantId = :tenantId)")
    Optional<PokaYokeDevice> findVisibleByCode(@Param("code") String code,
                                               @Param("tenantId") UUID tenantId);

    /** Pour numeroter le prochain dispositif du client. */
    long countByTenantId(UUID tenantId);

    boolean existsByCode(String code);
}
