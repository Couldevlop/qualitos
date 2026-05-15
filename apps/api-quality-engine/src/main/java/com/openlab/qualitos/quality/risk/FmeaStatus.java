package com.openlab.qualitos.quality.risk;

/**
 * Cycle de vie d'un projet FMEA.
 * DRAFT → ACTIVE ↔ DRAFT (revue) → ARCHIVED (terminal).
 */
public enum FmeaStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED
}
