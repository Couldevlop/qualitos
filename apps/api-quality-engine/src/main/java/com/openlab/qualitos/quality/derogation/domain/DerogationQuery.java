package com.openlab.qualitos.quality.derogation.domain;

import java.util.UUID;

/**
 * Les critères du registre, tels que la présentation les nomme : owner, produit,
 * client, statut.
 *
 * <p>Un objet et non six paramètres : six arguments de même type à la suite
 * s'inversent sans que le compilateur s'en aperçoive, et c'est le genre d'erreur
 * qui fait voir à un utilisateur les dossiers d'un autre.
 *
 * @param owner   le CRÉATEUR du dossier — c'est ainsi que la présentation définit
 *                l'{@code owner} du registre
 * @param visibleTo quand il est renseigné, la recherche se borne aux dossiers qui
 *                concernent ce compte : les siens et ceux qu'il doit signer.
 *                C'est l'ADR 0081 — sans le droit de tout voir, on ne voit que ce
 *                qui nous concerne, et hors de portée répond 404, jamais 403.
 */
public record DerogationQuery(
        UUID tenantId,
        UUID owner,
        String partReference,
        String customerName,
        DerogationStatus status,
        UUID visibleTo,
        int page,
        int size
) {
    /** Au-delà, un écran ne se lit plus et la requête coûte pour rien. */
    public static final int TAILLE_MAX = 200;

    public DerogationQuery {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId");
        }
        partReference = Texte.net(partReference);
        customerName = Texte.net(customerName);
        page = Math.max(page, 0);
        size = size <= 0 ? 20 : Math.min(size, TAILLE_MAX);
    }
}
