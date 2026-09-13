package com.openlab.qualitos.quality.nonconformity.eightd.application;

import java.util.UUID;

/**
 * Port — sceller puis revérifier l'empreinte d'un rapport 8D émis.
 *
 * <p>Signer et ancrer sont derrière le MÊME geste, comme pour le control plan :
 * les séparer laisserait le service décider du sort d'une signature obtenue quand
 * l'ancrage échoue, c'est-à-dire décider seul du sort d'une demi-preuve.
 *
 * <p>Conforme RGPD : seule l'empreinte part sur la chaîne — jamais le contenu du
 * rapport, jamais le nom des membres de l'équipe (§11.3).
 */
public interface EightDSealPort {

    /**
     * @param tenantId  le tenant propriétaire du rapport
     * @param sha256Hex l'empreinte du PDF rendu
     * @return la signature encodée et la référence de transaction
     * @throws RuntimeException si la signature ou l'ancrage échoue — l'émission doit
     *                          alors échouer aussi (§18.2 #5)
     */
    Seal seal(UUID tenantId, String sha256Hex);

    /**
     * Revalide une signature stockée sur l'empreinte stockée. Une enveloppe
     * illisible ou altérée répond {@code false}, jamais une exception : la route
     * publique de vérification ne doit pas distinguer « altéré » de « cassé ».
     */
    boolean verify(String signature, String sha256Hex);

    /** La preuve rendue : signature encodée, et référence opaque de transaction. */
    record Seal(String signature, String anchorTxRef) {
    }
}
