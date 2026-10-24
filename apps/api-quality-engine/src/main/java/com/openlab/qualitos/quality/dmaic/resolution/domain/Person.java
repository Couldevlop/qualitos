package com.openlab.qualitos.quality.dmaic.resolution.domain;

import java.util.UUID;

/**
 * Une personne de l'annuaire : son compte (sub du jeton) et son nom, recopié au
 * moment du choix pour que la fiche reste lisible si elle quitte l'annuaire.
 */
public record Person(UUID accountId, String name) {

    public static final int NAME_MAX = 250;
}
