package com.openlab.qualitos.quality.aiqms.domain;

/**
 * Cycle de vie d'un dossier QMS AI Act (Art. 17).
 *
 *   DRAFT → APPROVED → IN_FORCE → SUPERSEDED
 *   DRAFT|APPROVED → ARCHIVED
 */
public enum AiQmsStatus {
    DRAFT,
    APPROVED,
    IN_FORCE,
    SUPERSEDED,
    ARCHIVED
}
