package com.openlab.qualitos.quality.dashboards.annotations.application;

/**
 * Port — answers role questions about the current authenticated actor.
 * Implemented in infrastructure from the JWT-derived authorities; keeps the
 * use case free of Spring Security.
 */
public interface ActorRoles {

    /**
     * @return true if the current actor is a tenant/platform administrator
     *         (may moderate annotations authored by others).
     */
    boolean isTenantAdmin();
}
