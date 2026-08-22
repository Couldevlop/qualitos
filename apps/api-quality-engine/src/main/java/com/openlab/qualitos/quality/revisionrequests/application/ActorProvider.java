package com.openlab.qualitos.quality.revisionrequests.application;

import java.util.UUID;

/**
 * Port resolvant l'utilisateur courant (le {@code sub} du JWT).
 *
 * <p>Qui accepte ou refuse une revision est un fait d'audit : il vient toujours de
 * l'identite authentifiee, jamais du corps de la requete.
 */
public interface ActorProvider {

    UUID currentUserId();
}
