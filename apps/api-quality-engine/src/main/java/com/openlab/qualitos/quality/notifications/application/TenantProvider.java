package com.openlab.qualitos.quality.notifications.application;

import java.util.UUID;

/** Fournit le tenant courant (issu du JWT, jamais du body — OWASP A01). */
public interface TenantProvider {
    UUID requireTenantId();
}
