package com.openlab.qualitos.quality.standards.auditblanc.domain;

/**
 * Action de remédiation actionnable pour combler un écart de clause
 * (Standards Hub §8.4 onglet 7 : « plan de remédiation auto-créé »).
 * Value object PUR.
 *
 * <p>{@code targetModule} oriente vers le module QualitOS concerné (référentiel
 * transverse §3.6) : DOCUMENT_CONTROL pour produire une procédure, AUDIT pour
 * planifier un audit, PDCA pour piloter un cycle, TRAINING pour une formation…
 */
public record RemediationAction(
        String clauseCode,
        MockAuditCriticality criticality,
        String priority,
        String targetModule,
        String action) {

    public RemediationAction {
        if (clauseCode == null || clauseCode.isBlank()) {
            throw new IllegalArgumentException("clauseCode required");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action required");
        }
        if (priority == null || priority.isBlank()) {
            throw new IllegalArgumentException("priority required");
        }
        if (targetModule == null || targetModule.isBlank()) {
            throw new IllegalArgumentException("targetModule required");
        }
    }
}
