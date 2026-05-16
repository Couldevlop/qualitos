package com.openlab.qualitos.quality.retention.domain;

/**
 * <ul>
 *   <li>DRAFT — éditable, peut être activée ou supprimée.</li>
 *   <li>ACTIVE — règle en vigueur ; immutable (toute modif. requiert une nouvelle règle).</li>
 *   <li>ARCHIVED — terminal ; règle historique, encore référencée par les preuves.</li>
 * </ul>
 */
public enum RetentionRuleStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED
}
