package com.openlab.qualitos.quality.controlplan.application;

import java.util.UUID;

/**
 * Consigne au journal chaîné du tenant ce qui arrive au control plan.
 *
 * <p>Approuver rend le document opposable ; ouvrir une révision le dégèle. Ces
 * deux actes changent ce qui est applicable au poste, et un auditeur demandera
 * qui les a posés et quand. Le journal étant lui-même ancré périodiquement par
 * arbre de Merkle, les y inscrire suffit à les rendre infalsifiables.
 */
public interface ControlPlanAuditPort {

    void record(UUID tenantId, UUID actorId, String action, UUID planId,
                String summary, String detailsJson);
}
