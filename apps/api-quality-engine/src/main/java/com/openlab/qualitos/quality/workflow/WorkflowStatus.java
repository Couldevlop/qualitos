package com.openlab.qualitos.quality.workflow;

/**
 * Cycle de vie d'une définition de workflow BPMN (§5.4 Workflow designer no-code).
 *
 * <ul>
 *   <li>{@code DRAFT} — en cours d'édition, modifiable.</li>
 *   <li>{@code PUBLISHED} — figée et utilisable comme modèle de processus.</li>
 *   <li>{@code ARCHIVED} — retirée du catalogue actif (conservée pour l'historique).</li>
 * </ul>
 */
public enum WorkflowStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
