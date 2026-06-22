package com.openlab.qualitos.quality.marketplace.domain;

/**
 * État d'une installation de pack marketplace pour un tenant.
 * INSTALLED ↔ UNINSTALLED ; l'historique est préservé (jamais de suppression
 * physique — trace d'audit ISO 19011, comme les activations d'Industry Packs).
 */
public enum InstallationStatus {
    INSTALLED,
    UNINSTALLED
}
