package com.openlab.qualitos.quality.standards.normdoc.dossier.application;

import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DocumentationDossier;

/**
 * Port — publie les transitions d'un dossier documentaire vers le journal
 * d'audit chaîné (OWASP A09). Sans PII : seules des métadonnées.
 */
public interface DossierEventPublisher {

    void publish(DocumentationDossier dossier, Action action);

    enum Action { STARTED, GENERATED, FINALIZED }

    /** Implémentation neutre (tests). */
    final class NoOp implements DossierEventPublisher {
        @Override
        public void publish(DocumentationDossier dossier, Action action) {
            // no-op
        }
    }
}
