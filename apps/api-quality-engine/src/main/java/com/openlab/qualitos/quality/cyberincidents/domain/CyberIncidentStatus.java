package com.openlab.qualitos.quality.cyberincidents.domain;

/**
 * <ul>
 *   <li>DETECTED — découverte, compteurs NIS2 (24h/72h/30j) démarrés.</li>
 *   <li>ASSESSING — qualification et analyse.</li>
 *   <li>MITIGATED — endigué, mesures correctives en place.</li>
 *   <li>CLOSED — terminal : incident clos.</li>
 *   <li>REJECTED — terminal : faux positif.</li>
 * </ul>
 */
public enum CyberIncidentStatus {
    DETECTED,
    ASSESSING,
    MITIGATED,
    CLOSED,
    REJECTED
}
