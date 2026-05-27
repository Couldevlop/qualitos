package com.openlab.qualitos.quality.blockchain.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de lecture pour la vérification d'ancrage (ADR 0012). L'adapter
 * d'infrastructure lit dans les repos audit + reçus ; le domaine ne touche
 * jamais l'entité JPA.
 */
public interface AnchorReadPort {

    /** Référence de reçu (txRef) de l'événement identifié par son hash, si ancré. */
    Optional<String> txRefForEvent(UUID tenantId, String integrityHash);

    /** Hashes d'intégrité du lot ancré sous {@code txRef}, en ordre de séquence. */
    List<String> integrityHashesForTxRef(UUID tenantId, String txRef);

    /** Reçu signé associé à {@code txRef}, si présent et appartenant au tenant. */
    Optional<ReceiptView> receipt(UUID tenantId, String txRef);

    /** Tenants ayant des événements non ancrés (pilotage du scheduler). */
    List<UUID> tenantsWithUnanchoredEvents();

    /** Vue minimale d'un reçu pour la vérification (Merkle root + signature). */
    record ReceiptView(String merkleRoot, String signature) {}
}
