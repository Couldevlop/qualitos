package com.openlab.qualitos.core.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt des abonnements.
 *
 * <p>Toutes les lectures utiles portent sur les abonnements VIVANTS
 * ({@code cancelledAt IS NULL}) : c'est l'exacte moitié de la table que
 * couvrent les index partiels de la migration V6. Les résiliés restent
 * lisibles par {@link JpaRepository#findById} — l'historique justifie les
 * factures passées — mais aucune méthode ne les mélange aux vivants, ce qui
 * ferait facturer un contrat clos.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.tenantId = :tenantId
              AND s.moduleCode = :moduleCode
              AND s.cancelledAt IS NULL
            """)
    Optional<Subscription> findLiveByTenantAndModule(
            @Param("tenantId") UUID tenantId,
            @Param("moduleCode") String moduleCode);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.tenantId = :tenantId
              AND s.cancelledAt IS NULL
            ORDER BY s.moduleCode
            """)
    List<Subscription> findLiveByTenant(@Param("tenantId") UUID tenantId);
}
