package com.openlab.qualitos.quality.change;

/**
 * Cycle de vie d'une demande de changement (§4.8).
 *
 * DRAFT         : brouillon, modifiable
 * SUBMITTED     : remis au workflow, en attente d'approbations
 * UNDER_REVIEW  : au moins un approbateur a statué, mais pas tous
 * APPROVED      : tous les approbateurs ont approuvé
 * REJECTED      : au moins un approbateur a rejeté — terminal
 * IMPLEMENTED   : déployé en production — terminal (transition explicite)
 * CANCELLED     : abandonné par le demandeur — terminal
 */
public enum ChangeRequestStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    IMPLEMENTED,
    CANCELLED
}
