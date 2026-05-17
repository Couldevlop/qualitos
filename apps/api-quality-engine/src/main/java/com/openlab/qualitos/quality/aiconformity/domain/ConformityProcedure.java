package com.openlab.qualitos.quality.aiconformity.domain;

/**
 * Procédure d'évaluation de conformité AI Act (Art. 43).
 *
 * - INTERNAL_CONTROL (Annexe VI) : provider auto-évalue, pas de notified body.
 * - NOTIFIED_BODY (Annexe VII) : évaluation par un organisme notifié,
 *   obligatoire pour certains systèmes HIGH-risk (Annexe III §1.a).
 */
public enum ConformityProcedure {
    INTERNAL_CONTROL,
    NOTIFIED_BODY
}
