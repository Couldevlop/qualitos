package com.openlab.qualitos.quality.derogation.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Section 5 — à qui la signature est demandée, et ce qu'il a signé.
 *
 * <p><b>La signature est déclarative</b> : un nom saisi et un tracé. Ni second
 * facteur, ni signataire au sens de l'ADR 0099, ni sceau cryptographique —
 * arbitrage explicite, cohérent avec le PDF non scellé. La seule pièce opposable
 * du dossier reste le PDF que le client renvoie signé de son côté.
 *
 * @param managerAccountId   le manager choisi dans l'annuaire : lui, et lui seul,
 *                           peut signer ou renvoyer ce dossier
 * @param managerAccountName son nom FIGÉ à la désignation — un compte renommé ne
 *                           doit pas réécrire une signature déjà donnée
 * @param signedByName       le nom que le manager SAISIT en signant
 * @param signaturePng       son tracé, en PNG
 * @param amendReason        le motif du renvoi « À modifier »
 */
public record ManagerApproval(
        UUID managerAccountId,
        String managerAccountName,
        String signedByName,
        byte[] signaturePng,
        Instant signedAt,
        String amendReason
) {
    /** Aucune signature demandée : l'état d'un dossier qui s'ouvre. */
    public static ManagerApproval aucune() {
        return new ManagerApproval(null, null, null, null, null, null);
    }

    /** Le dossier est confié à ce manager, et le motif du tour précédent tombe. */
    public ManagerApproval confieA(UUID compte, String nom) {
        return new ManagerApproval(compte, nom, null, null, null, null);
    }

    /** Le manager signe. */
    public ManagerApproval signee(String nom, byte[] trace, Instant quand) {
        return new ManagerApproval(managerAccountId, managerAccountName,
                Texte.net(nom), trace, quand, null);
    }

    /** Le manager renvoie pour correction : la signature reste absente. */
    public ManagerApproval renvoyee(String motif) {
        return new ManagerApproval(managerAccountId, managerAccountName,
                null, null, null, Texte.net(motif));
    }

    /**
     * La signature est effacée, le manager désigné reste.
     *
     * <p>Garder le tracé sur une fiche qu'on va modifier laisserait croire que le
     * manager a signé ce qui va être écrit.
     */
    public ManagerApproval sansSignature() {
        return new ManagerApproval(managerAccountId, managerAccountName,
                null, null, null, amendReason);
    }

    public boolean estSignee() {
        return signedAt != null;
    }

    /**
     * Ce compte est-il le manager désigné ?
     *
     * <p>Un dossier sans manager désigné n'en a aucun : personne ne peut le
     * signer, et {@code null} ne vaut pas joker.
     */
    public boolean estLeManager(UUID compte) {
        return managerAccountId != null && managerAccountId.equals(compte);
    }
}
