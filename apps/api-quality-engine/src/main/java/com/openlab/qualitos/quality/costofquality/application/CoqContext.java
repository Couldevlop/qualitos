package com.openlab.qualitos.quality.costofquality.application;

import java.util.UUID;

/**
 * Port : qui agit, et pour quel client.
 *
 * <p>Les deux viennent du jeton validé, jamais du corps de la requête
 * (CLAUDE.md §18.2) — la couche application ne voit que cette abstraction.
 */
public interface CoqContext {

    UUID requireTenantId();

    UUID requireActorId();
}
