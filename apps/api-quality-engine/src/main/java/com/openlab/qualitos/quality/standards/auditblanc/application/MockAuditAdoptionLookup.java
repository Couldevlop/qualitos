package com.openlab.qualitos.quality.standards.auditblanc.application;

import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditClause;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de lecture de la matière d'audit : pour une adoption de norme du tenant
 * courant, résout l'identité de la norme et calcule les clauses à risque avec
 * leur <b>état de preuve réel</b> (confrontation aux preuves liées par le tenant
 * dans le Standards Hub) — Standards Hub §8.4 onglet 7.
 *
 * <p>L'adapter d'infrastructure DOIT filtrer par tenant (l'adoption est
 * tenant-scoped) ; un identifiant d'adoption d'un autre tenant renvoie
 * {@link Optional#empty()} (le service mappe en 404, OWASP A01).
 */
public interface MockAuditAdoptionLookup {

    Optional<AdoptionMatter> findMatter(UUID adoptionId);

    /**
     * Matière d'un audit blanc : la norme + ses clauses (avec décompte de
     * preuves tenant). {@code industry} sert à contextualiser le prompt IA.
     */
    record AdoptionMatter(
            UUID adoptionId,
            UUID standardId,
            String standardCode,
            String standardName,
            String industry,
            List<MockAuditClause> clauses) {
    }
}
