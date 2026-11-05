package com.openlab.qualitos.core.tenant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * L'identité d'un client, par son identifiant de client — qui est la clé
 * primaire. Aucune méthode de recherche n'est nécessaire : il n'y a jamais de
 * liste à parcourir, seulement une ligne à lire.
 */
public interface TenantIdentityRepository extends JpaRepository<TenantIdentity, UUID> {
}
