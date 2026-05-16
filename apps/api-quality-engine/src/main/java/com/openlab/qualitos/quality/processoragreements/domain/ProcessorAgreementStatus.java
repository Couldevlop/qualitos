package com.openlab.qualitos.quality.processoragreements.domain;

/**
 * Cycle de vie d'un accord de sous-traitance (DPA — Art. 28).
 * <ul>
 *   <li>DRAFT — en cours de négociation/rédaction.</li>
 *   <li>ACTIVE — contrat signé et en vigueur.</li>
 *   <li>TERMINATED — résilié explicitement (terminal).</li>
 *   <li>EXPIRED — expiration automatique au passage de expirationDate (terminal).</li>
 * </ul>
 */
public enum ProcessorAgreementStatus {
    DRAFT,
    ACTIVE,
    TERMINATED,
    EXPIRED
}
