package com.openlab.qualitos.quality.controlplan.application;

import java.util.UUID;

/**
 * Port — sceller l'empreinte d'un control plan approuvé.
 *
 * <p>Deux gestes, un seul port : signer l'empreinte (Ed25519 + ML-DSA-65) et
 * l'ancrer. Les séparer aurait laissé le service décider quoi faire d'une
 * signature obtenue quand l'ancrage échoue — c'est-à-dire décider seul du sort
 * d'une demi-preuve.
 *
 * <p>Conforme RGPD : seule l'empreinte part sur la chaîne, jamais le contenu du
 * document ni le nom de qui l'a approuvé.
 */
public interface ControlPlanSealPort {

    /**
     * @param tenantId  le tenant propriétaire du document
     * @param sha256Hex l'empreinte du plan et de ses lignes
     * @return la signature encodée et la référence de transaction
     * @throws RuntimeException si la signature ou l'ancrage échoue — l'approbation
     *                          doit alors échouer aussi (CLAUDE.md §18.2 #5)
     */
    Seal seal(UUID tenantId, String sha256Hex);

    /** La preuve rendue : signature encodée, et référence opaque de transaction. */
    record Seal(String signature, String anchorTxRef) {
    }
}
