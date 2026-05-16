package com.openlab.qualitos.quality.aiactfria.domain;

/**
 * Statuts du cycle de vie d'une FRIA (Fundamental Rights Impact Assessment).
 * Art. 27 — l'évaluation est conduite, soumise pour validation interne,
 * approuvée par le responsable conformité, puis archivée à la fin
 * du déploiement de l'IA.
 */
public enum FriaStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    ARCHIVED
}
