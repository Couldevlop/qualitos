package com.openlab.qualitos.quality.standards.auditblanc.application;

import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRun;

/**
 * Port d'audit : journalise l'exécution d'un audit blanc (OWASP A09). L'adapter
 * route vers le journal d'audit chaîné/ancrable. Aucune PII : seules des
 * métadonnées (code norme, scores, décomptes) sont journalisées.
 */
public interface MockAuditEventPublisher {

    void published(MockAuditRun run);

    /** Implémentation neutre (tests, contexte sans audit). */
    final class NoOp implements MockAuditEventPublisher {
        @Override
        public void published(MockAuditRun run) {
            // no-op
        }
    }
}
