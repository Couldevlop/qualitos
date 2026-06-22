package com.openlab.qualitos.quality.standards.normdoc.dossier.domain;

import java.util.List;

/**
 * Plan de génération d'un dossier documentaire : la liste ordonnée des pièces à
 * rédiger (Manuel Qualité multi-sections, Politique Qualité, procédures
 * documentées requises). Domaine pur, sans dépendance.
 */
public record DossierPlan(List<DossierDocument> documents) {

    public DossierPlan {
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("a dossier plan needs at least one document");
        }
        documents = List.copyOf(documents);
    }
}
