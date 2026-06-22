package com.openlab.qualitos.quality.marketplace.application;

import java.util.UUID;

/**
 * Port — résout l'acteur authentifié courant (sub du JWT) pour les actions
 * marketplace qui ne sont PAS réservées au super-admin (soumission partenaire,
 * installation tenant). L'implémentation lit l'identité du JWT, jamais le body
 * (OWASP A01).
 */
public interface CurrentActorProvider {
    /** @return le sub du JWT typé UUID, ou lève si absent. */
    UUID requireActorId();
}
