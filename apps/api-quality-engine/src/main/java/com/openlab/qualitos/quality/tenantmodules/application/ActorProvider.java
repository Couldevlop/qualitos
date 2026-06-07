package com.openlab.qualitos.quality.tenantmodules.application;

import java.util.UUID;

/**
 * Port résolvant l'acteur de l'action courante (qui a activé/suspendu un module…).
 *
 * <p>Invariant de sécurité (OWASP A01 — H2) : l'acteur provient TOUJOURS de
 * l'identité authentifiée (le {@code sub} du JWT), jamais du corps de requête,
 * qui est falsifiable. Le service métier ne lit donc plus d'{@code actor} dans le
 * DTO web ; il l'obtient via ce port.</p>
 */
public interface ActorProvider {

    /** @return l'UUID de l'utilisateur courant, ou lève si le contexte est absent. */
    UUID requireActorId();
}
