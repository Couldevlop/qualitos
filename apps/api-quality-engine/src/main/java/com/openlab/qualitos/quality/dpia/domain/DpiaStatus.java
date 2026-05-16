package com.openlab.qualitos.quality.dpia.domain;

/**
 * <ul>
 *   <li>DRAFT — éditable, peut être démarré ou supprimé.</li>
 *   <li>IN_PROGRESS — analyse en cours, peut être renvoyé en DRAFT pour correction.</li>
 *   <li>DPO_REVIEW — soumise au DPO pour avis (Art. 35§2).</li>
 *   <li>APPROVED — terminal côté workflow ; consultation Art. 36 peut être requise.</li>
 *   <li>REJECTED — terminal ; le traitement ne doit pas être mis en œuvre tel quel.</li>
 *   <li>ARCHIVED — terminal historique ; DPIA conservée pour la traçabilité.</li>
 * </ul>
 */
public enum DpiaStatus {
    DRAFT,
    IN_PROGRESS,
    DPO_REVIEW,
    APPROVED,
    REJECTED,
    ARCHIVED
}
