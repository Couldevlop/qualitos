package com.openlab.qualitos.quality.nis2measures.domain;

/**
 * <ul>
 *   <li>PLANNED — mesure identifiée, à mettre en œuvre.</li>
 *   <li>IN_PROGRESS — implémentation en cours.</li>
 *   <li>IMPLEMENTED — mesure déployée, en attente de vérification.</li>
 *   <li>VERIFIED — mesure vérifiée (audit / revue effective).</li>
 *   <li>DEPRECATED — terminal : remplacée ou abandonnée.</li>
 * </ul>
 */
public enum Nis2MeasureStatus {
    PLANNED,
    IN_PROGRESS,
    IMPLEMENTED,
    VERIFIED,
    DEPRECATED
}
