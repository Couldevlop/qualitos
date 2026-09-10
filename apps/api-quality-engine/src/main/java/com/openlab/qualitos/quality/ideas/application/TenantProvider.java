package com.openlab.qualitos.quality.ideas.application;

import java.util.UUID;

/** Port resolvant le tenant courant, sans jamais depender de Spring cote application. */
public interface TenantProvider {

    /** @return l'UUID du tenant courant, ou leve si le contexte est absent. */
    UUID requireTenantId();
}
