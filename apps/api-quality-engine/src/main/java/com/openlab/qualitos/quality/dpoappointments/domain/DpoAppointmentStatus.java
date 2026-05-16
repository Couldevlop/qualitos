package com.openlab.qualitos.quality.dpoappointments.domain;

/**
 * Cycle de vie d'une désignation de DPO.
 * <ul>
 *   <li>PROPOSED — désignation préparée, en attente de notification autorité
 *       de contrôle / formalisation.</li>
 *   <li>ACTIVE — désignation effective ; le DPO est en fonction.</li>
 *   <li>ENDED — terminée (départ, fin de mandat).</li>
 *   <li>CANCELLED — terminale, désignation annulée avant prise d'effet.</li>
 * </ul>
 */
public enum DpoAppointmentStatus {
    PROPOSED,
    ACTIVE,
    ENDED,
    CANCELLED
}
