package com.openlab.qualitos.quality.aiact.domain;

/**
 * <ul>
 *   <li>DRAFT — fiche en cours de préparation.</li>
 *   <li>REGISTERED — enregistré (classification + obligations documentées),
 *       pas encore en production.</li>
 *   <li>IN_USE — système en production.</li>
 *   <li>DECOMMISSIONED — terminal — système retiré du service.</li>
 *   <li>WITHDRAWN — terminal — abandonné avant mise en service.</li>
 * </ul>
 */
public enum AiSystemStatus {
    DRAFT,
    REGISTERED,
    IN_USE,
    DECOMMISSIONED,
    WITHDRAWN
}
