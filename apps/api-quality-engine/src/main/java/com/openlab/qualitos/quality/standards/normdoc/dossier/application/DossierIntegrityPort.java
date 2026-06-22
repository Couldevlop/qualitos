package com.openlab.qualitos.quality.standards.normdoc.dossier.application;

import java.util.UUID;

/**
 * Port — scelle l'intégrité d'un dossier finalisé : calcule l'empreinte SHA-256
 * du contenu agrégé, la signe (hybride Ed25519 + ML-DSA-65) et ancre la
 * référence en blockchain (preuve audit-ready, §8.7 / §11.3). Implémenté par un
 * adapter qui réutilise l'infra crypto/blockchain existante.
 */
public interface DossierIntegrityPort {

    /**
     * @param tenantId        tenant courant (issu du JWT).
     * @param canonicalContent contenu canonique agrégé du dossier (Markdown concaténé).
     * @return l'empreinte, la signature encodée et la référence d'ancrage.
     */
    Sealed seal(UUID tenantId, String canonicalContent);

    record Sealed(String sha256, String signature, String anchorTxRef) {}
}
