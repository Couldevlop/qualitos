package com.openlab.qualitos.quality.aipmm.domain;

/**
 * Cycle de vie d'un plan de surveillance post-marché (AI Act Art. 72).
 *
 *   DRAFT → ACTIVE → SUSPENDED → ACTIVE
 *   ACTIVE|SUSPENDED → CLOSED
 *   DRAFT → CLOSED
 */
public enum PmmPlanStatus {
    DRAFT,
    ACTIVE,
    SUSPENDED,
    CLOSED
}
