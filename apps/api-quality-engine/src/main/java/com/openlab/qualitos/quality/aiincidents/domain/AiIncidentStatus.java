package com.openlab.qualitos.quality.aiincidents.domain;

/**
 * Cycle de vie d'un signalement d'incident IA.
 *
 *  DETECTED → INVESTIGATING → NOTIFIED_REGULATOR → CLOSED
 *  DETECTED → DISMISSED
 *  INVESTIGATING → DISMISSED
 */
public enum AiIncidentStatus {
    DETECTED,
    INVESTIGATING,
    NOTIFIED_REGULATOR,
    CLOSED,
    DISMISSED
}
