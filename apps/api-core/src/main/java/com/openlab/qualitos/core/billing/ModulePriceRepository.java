package com.openlab.qualitos.core.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt du tarif des modules. La recherche porte sur la clé naturelle
 * complète (module, palier, périodicité) — c'est exactement la contrainte
 * {@code uk_module_price} de la migration V5, et le seul moyen de retrouver
 * SANS AMBIGUÏTÉ le tarif applicable à un abonnement donné.
 *
 * <p>{@code @Query} explicite plutôt qu'une dérivation
 * {@code findByModuleCodeAndBillingTierAndPeriod} : le nom {@code find} est
 * celui attendu par {@link ModulePriceService} (et par son test), et Spring
 * Data ne peut pas dériver de critères d'un nom de méthode qui ne contient
 * pas {@code By}.
 */
@Repository
public interface ModulePriceRepository extends JpaRepository<ModulePrice, UUID> {

    @Query("""
            SELECT p FROM ModulePrice p
            WHERE p.moduleCode = :moduleCode
              AND p.billingTier = :billingTier
              AND p.period = :period
            """)
    Optional<ModulePrice> find(
            @Param("moduleCode") String moduleCode,
            @Param("billingTier") BillingTier billingTier,
            @Param("period") BillingPeriod period);
}
