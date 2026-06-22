package com.openlab.qualitos.quality.academy.domain;

/**
 * Cycle de vie d'un cours e-learning (§19.3).
 *
 * <p>{@code DRAFT} : en cours d'autoring, non visible des apprenants.
 * {@code PUBLISHED} : ouvert à l'inscription. {@code ARCHIVED} : retiré du
 * catalogue (les inscriptions et certificats existants restent valides).</p>
 */
public enum CourseStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
