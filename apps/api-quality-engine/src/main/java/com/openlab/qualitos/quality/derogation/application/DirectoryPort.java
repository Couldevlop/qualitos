package com.openlab.qualitos.quality.derogation.application;

import java.util.Optional;
import java.util.UUID;

/**
 * L'annuaire : le nom d'affichage et l'adresse de courriel d'un compte.
 *
 * <p>Le manager se choisit dans l'annuaire, et son nom est figé sur le dossier à
 * la désignation : un compte renommé ne doit pas réécrire une signature déjà
 * donnée. Son adresse, elle, se relit à chaque envoi — c'est une coordonnée, pas
 * une trace.
 */
public interface DirectoryPort {

    Optional<String> displayNameOf(UUID accountId);

    /** L'adresse du compte, si l'annuaire en connaît une. */
    Optional<String> emailOf(UUID accountId);

    /**
     * La fonction du compte dans l'organisation, en toutes lettres.
     *
     * <p>« Responsable qualité », « Audit interne » : ce que la personne FAIT,
     * tiré des rôles qu'on lui a donnés à son invitation. Vide quand elle n'en
     * porte aucun — une fonction par défaut nommerait, dans un document remis
     * au client, un poste que personne n'a renseigné.
     */
    Optional<String> fonctionOf(UUID accountId);
}
