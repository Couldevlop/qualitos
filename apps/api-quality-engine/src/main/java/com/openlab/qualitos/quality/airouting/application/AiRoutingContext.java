package com.openlab.qualitos.quality.airouting.application;

import java.util.UUID;

/**
 * Port : pour quel tenant, et qui agit. Les deux viennent du jeton validé, jamais
 * du corps de la requête (§18.2).
 */
public interface AiRoutingContext {

    UUID requireTenantId();

    UUID requireActorId();
}
