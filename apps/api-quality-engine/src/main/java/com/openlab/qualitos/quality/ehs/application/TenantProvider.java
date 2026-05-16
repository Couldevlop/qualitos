package com.openlab.qualitos.quality.ehs.application;

import java.util.UUID;

/**
 * Port d'identité — l'application demande le tenant courant sans connaître le
 * mécanisme d'extraction (JWT, header, etc.). Implémenté en infrastructure par
 * un adapter qui lit {@code TenantContext}.
 */
public interface TenantProvider {
    UUID requireTenantId();
}
