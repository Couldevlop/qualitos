package com.openlab.qualitos.quality.change.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Un rôle de validation des changements, propre au client : « Responsable de
 * service », « HSE »… avec ses titulaires et les classifications qui l'exigent.
 *
 * <p>Ce n'est PAS un rôle d'habilitation de la plateforme : c'est une fonction
 * dans l'organisation du client, que l'administrateur relie à des personnes.
 * N'importe lequel des titulaires (principal ou suppléant) statue pour le rôle.
 *
 * @param id          identifiant stable ; {@code null} pour un rôle pas encore enregistré
 * @param label       libellé affiché, unique dans le client, 100 caractères au plus
 * @param requiredFor classifications pour lesquelles ce rôle doit valider
 * @param holders     titulaires, le principal en premier
 */
public record ValidationRole(UUID id, String label, Set<MocClassification> requiredFor, List<RoleHolder> holders) {

    public static final int LABEL_MAX = 100;

    public ValidationRole {
        requiredFor = requiredFor == null || requiredFor.isEmpty()
                ? Set.of() : Set.copyOf(EnumSet.copyOf(requiredFor));
        holders = holders == null ? List.of() : List.copyOf(holders);
    }

    public boolean requiredFor(MocClassification classification) {
        return requiredFor.contains(classification);
    }

    public boolean heldBy(UUID userId) {
        return userId != null && holders.stream().anyMatch(h -> h.userId().equals(userId));
    }

    /**
     * Un titulaire de l'annuaire du client.
     *
     * @param userId l'identifiant de connexion (le {@code sub} du jeton)
     * @param label  ce que l'annuaire en montre (son nom), recopié pour l'affichage ;
     *               une adresse pour les titulaires nommés avant que le nom soit lisible
     * @param email  son adresse, pour le courriel ; {@code null} si inconnue
     */
    public record RoleHolder(UUID userId, String label, String email) {

        public static final int LABEL_MAX = 200;
        public static final int EMAIL_MAX = 320;

        public RoleHolder {
            email = email == null || email.isBlank() ? null : email.strip();
        }

        /** Un titulaire sans adresse connue. */
        public RoleHolder(UUID userId, String label) {
            this(userId, label, null);
        }

        /**
         * L'adresse où écrire : la sienne, ou le libellé quand c'en est une
         * (titulaire nommé avant que l'adresse ait sa colonne) ; sinon aucune.
         */
        public String mailAddress() {
            if (email != null) {
                return email;
            }
            return label != null && label.contains("@") ? label : null;
        }
    }
}
