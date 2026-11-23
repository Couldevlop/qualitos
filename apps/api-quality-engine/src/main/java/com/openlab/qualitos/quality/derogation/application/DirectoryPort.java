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
}
