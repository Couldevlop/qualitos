package com.openlab.qualitos.quality.riskregister.application;

import java.util.UUID;

/**
 * Port : qui agit, et pour quel client.
 *
 * <p>Les deux viennent du jeton validé, jamais du corps de la requête
 * (CLAUDE.md §18.2).
 */
public interface RegisterContext {

    UUID requireTenantId();

    UUID requireActorId();
}
