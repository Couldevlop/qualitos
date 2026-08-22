package com.openlab.qualitos.quality.product.application;

import java.util.UUID;

/**
 * Port résolvant l'utilisateur courant (le {@code sub} du JWT).
 *
 * <p>Invariant de sécurité (OWASP A01) : l'acteur d'une écriture (qui a créé un
 * produit…) provient TOUJOURS de l'identité authentifiée, jamais du corps de
 * requête, qui est falsifiable.
 */
public interface ActorProvider {

    UUID currentUserId();
}
