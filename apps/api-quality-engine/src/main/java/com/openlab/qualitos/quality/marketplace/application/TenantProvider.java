package com.openlab.qualitos.quality.marketplace.application;

import java.util.UUID;

/**
 * Port — résout le tenant courant (issu du JWT) pour les opérations multi-tenant
 * (installation/désinstallation de pack). Lève si le contexte tenant est absent.
 */
public interface TenantProvider {
    UUID requireTenantId();
}
