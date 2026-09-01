package com.openlab.qualitos.core.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Dépôt du profil de facturation. Un seul enregistrement par tenant (voir la
 * contrainte {@code uq_billing_profiles_tenant}), d'où l'absence de toute
 * méthode "findAllByTenantId" : elle n'aurait jamais qu'un résultat.
 */
@Repository
public interface BillingProfileRepository extends JpaRepository<BillingProfile, UUID> {

    Optional<BillingProfile> findByTenantId(UUID tenantId);
}
