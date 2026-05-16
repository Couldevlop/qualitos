package com.openlab.qualitos.quality.ropa.domain;

/**
 * Cycle de vie d'une activité de traitement.
 * <ul>
 *   <li>DRAFT — éditable, modifiable.</li>
 *   <li>ACTIVE — immutable ; représente le traitement actuellement opéré.</li>
 *   <li>ARCHIVED — terminal ; conservé pour la traçabilité historique.</li>
 * </ul>
 */
public enum ProcessingActivityStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED
}
