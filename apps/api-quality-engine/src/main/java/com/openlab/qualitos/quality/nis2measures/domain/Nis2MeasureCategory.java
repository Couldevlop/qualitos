package com.openlab.qualitos.quality.nis2measures.domain;

/**
 * Les 10 catégories de mesures obligatoires (NIS2 Art. 21.2.a-j) que les
 * entités essentielles et importantes doivent mettre en œuvre.
 */
public enum Nis2MeasureCategory {
    /** (a) politiques d'analyse de risque et de sécurité des SI. */
    RISK_ANALYSIS,
    /** (b) gestion des incidents. */
    INCIDENT_HANDLING,
    /** (c) continuité d'activité (sauvegarde, PRA, gestion de crise). */
    BUSINESS_CONTINUITY,
    /** (d) sécurité de la chaîne d'approvisionnement. */
    SUPPLY_CHAIN_SECURITY,
    /** (e) sécurité dans l'acquisition / développement / maintenance. */
    SECURE_DEVELOPMENT,
    /** (f) politiques d'évaluation de l'efficacité des mesures. */
    EFFECTIVENESS_ASSESSMENT,
    /** (g) pratiques d'hygiène cyber et formation. */
    CYBER_HYGIENE_TRAINING,
    /** (h) politiques et procédures de cryptographie (incl. chiffrement). */
    CRYPTOGRAPHY,
    /** (i) sécurité RH, contrôle d'accès, gestion d'actifs. */
    HR_AND_ACCESS_CONTROL,
    /** (j) MFA / solutions d'authentification continue / communications sécurisées. */
    MFA_AND_COMMUNICATIONS
}
