package com.openlab.qualitos.quality.consent.domain;

/**
 * États d'un consentement RGPD (Art. 7).
 * <ul>
 *   <li>GRANTED : consentement donné, encore valide.</li>
 *   <li>WITHDRAWN : retiré par la personne — terminal, irréversible. Un
 *       nouveau consentement nécessite un nouvel enregistrement.</li>
 *   <li>EXPIRED : terminal — consentement à durée limitée arrivé à terme.</li>
 * </ul>
 */
public enum ConsentStatus {
    GRANTED,
    WITHDRAWN,
    EXPIRED
}
