package com.openlab.qualitos.quality.ideas.application;

import java.util.Optional;
import java.util.UUID;

/**
 * Port resolvant l'acteur de l'action courante (qui depose, qui vote...).
 *
 * <p>Invariant de securite (OWASP A01, CLAUDE.md §18.2) : l'acteur vient
 * TOUJOURS de l'identite authentifiee, jamais du corps de la requete, qui est
 * falsifiable. Le domaine ne connait donc que cette abstraction, jamais le JWT.
 */
public interface ActorProvider {

    /** @return l'UUID de l'utilisateur courant, ou leve si le contexte est absent. */
    UUID requireActorId();

    /** @return le nom d'affichage de l'utilisateur courant, ou vide si indisponible. */
    Optional<String> actorDisplayName();
}
