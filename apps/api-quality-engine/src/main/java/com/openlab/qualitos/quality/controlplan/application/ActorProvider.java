package com.openlab.qualitos.quality.controlplan.application;

import java.util.UUID;

/**
 * Port résolvant l'utilisateur courant (le {@code sub} du JWT).
 *
 * <p>Invariant de sécurité (OWASP A01) : celui qui approuve un plan vient
 * TOUJOURS de l'identité authentifiée, jamais du corps de requête. Une signature
 * d'approbation forgée par le client ne vaudrait rien devant un auditeur.
 */
public interface ActorProvider {

    UUID currentUserId();
}
