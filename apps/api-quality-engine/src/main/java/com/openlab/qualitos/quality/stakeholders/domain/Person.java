package com.openlab.qualitos.quality.stakeholders.domain;

import java.util.UUID;

/**
 * Une personne de l'annuaire : son compte de connexion, et son nom recopié.
 *
 * <p>Le nom est une COPIE, pas une jointure : la fiche doit rester lisible des
 * années plus tard, quand le compte a été renommé ou a disparu de l'annuaire
 * (ADR 0091).
 */
public record Person(UUID accountId, String name) {

    public static final int NAME_MAX = 255;

    public Person {
        if (accountId == null) {
            throw new IllegalArgumentException("a person is designated by an account");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("a person is displayed by a name");
        }
        name = name.strip();
        if (name.length() > NAME_MAX) {
            name = name.substring(0, NAME_MAX);
        }
    }
}
