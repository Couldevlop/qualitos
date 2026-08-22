package com.openlab.qualitos.quality.capa.effectiveness.application;

import java.util.UUID;

/**
 * Port — le tenant du contexte de sécurité.
 *
 * <p>Il vient du jeton validé, jamais du corps ni de l'URL de la requête
 * (CLAUDE.md §18.2 #2).
 */
public interface TenantProvider {

    UUID requireTenantId();
}
