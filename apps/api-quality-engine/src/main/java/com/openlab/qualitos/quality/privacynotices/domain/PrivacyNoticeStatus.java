package com.openlab.qualitos.quality.privacynotices.domain;

/**
 * <ul>
 *   <li>DRAFT — éditable, peut être publié ou supprimé.</li>
 *   <li>PUBLISHED — version actuellement communiquée aux personnes concernées.
 *       Immutable (toute évolution = nouvelle version DRAFT).</li>
 *   <li>ARCHIVED — terminal historique ; conserve la mention pour la
 *       traçabilité (preuve de ce qui a été affiché).</li>
 * </ul>
 */
public enum PrivacyNoticeStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
