package com.openlab.qualitos.quality.apqp;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/**
 * Qui agit, d'après le sujet du jeton — jamais d'après le corps de la requête.
 *
 * <p>Trois contrôleurs du module posaient la même méthode privée. La partager
 * évite qu'une quatrième variante finisse par lire l'acteur ailleurs, ce qui est
 * exactement ce que la règle §18.2.2 interdit.
 *
 * <p>Si le sujet n'est pas un UUID, l'acteur reste vide : mieux vaut une action
 * sans auteur qu'un auteur inventé.
 */
final class ApqpActor {

    private ApqpActor() {}

    static UUID de(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            return null;
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
