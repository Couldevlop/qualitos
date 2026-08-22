package com.openlab.qualitos.quality.training.competencymatrix.application;

import java.util.UUID;

/** Port — le tenant du contexte de securite, issu du jeton valide (§18.2 #2). */
public interface TenantProvider {

    UUID requireTenantId();
}
