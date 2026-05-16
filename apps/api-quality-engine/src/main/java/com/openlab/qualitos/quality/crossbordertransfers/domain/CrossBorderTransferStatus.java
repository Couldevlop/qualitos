package com.openlab.qualitos.quality.crossbordertransfers.domain;

/**
 * <ul>
 *   <li>DRAFT — préparation, éditable.</li>
 *   <li>ACTIVE — transfert en cours.</li>
 *   <li>SUSPENDED — temporairement suspendu (alerte juridique, audit en cours).</li>
 *   <li>TERMINATED — terminal, transfert clos.</li>
 * </ul>
 */
public enum CrossBorderTransferStatus {
    DRAFT,
    ACTIVE,
    SUSPENDED,
    TERMINATED
}
