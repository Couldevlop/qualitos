package com.openlab.qualitos.quality.standards;

import java.util.List;

/**
 * Trame générique de roadmap de certification (CLAUDE.md §8.5) : les 19 étapes
 * chronologiques d'un projet de certification ISO/HLS, avec durée typique,
 * livrables clés, responsable et modules QualitOS impliqués.
 *
 * Industry-agnostic : la même trame s'instancie pour toute norme certifiable.
 * Les variantes spécifiques (MDR, FDA, HACCP… §8.6) pourront enrichir cette base.
 */
public final class RoadmapTemplate {

    private RoadmapTemplate() {}

    /** Définition immuable d'une étape de la trame. */
    public record StageDefinition(
            int stepNumber,
            String name,
            String description,
            String typicalDuration,
            String deliverables,
            String responsibleRole,
            String involvedModules) {}

    public static final List<StageDefinition> STAGES = List.of(
        new StageDefinition(1, "Cadrage & engagement direction",
            "Définir le périmètre, le budget, le planning et obtenir l'engagement formel de la direction.",
            "2-4 sem", "Lettre d'engagement direction, périmètre, budget, planning",
            "Direction, Pilote certification", "Document Control, PDCA"),
        new StageDefinition(2, "Diagnostic initial / gap analysis",
            "Évaluer l'écart entre les pratiques actuelles et les exigences de la norme.",
            "3-5 sem", "Rapport d'écarts, matrice de conformité initiale",
            "Pilote, Auditeur interne", "Audit, Standards Hub (audit blanc IA)"),
        new StageDefinition(3, "Définition de la politique & objectifs",
            "Formaliser la politique, les objectifs SMART et les indicateurs cibles.",
            "2 sem", "Politique signée, objectifs SMART, indicateurs cibles",
            "Direction Qualité", "Document Control, Catalogue KPI"),
        new StageDefinition(4, "Analyse du contexte & parties intéressées",
            "Analyser le contexte interne/externe et recenser les parties intéressées.",
            "2-3 sem", "SWOT/PESTEL, registre des parties intéressées",
            "Pilote + équipes", "Document Control"),
        new StageDefinition(5, "Analyse des risques & opportunités",
            "Cartographier les risques et opportunités et définir les plans de traitement.",
            "3-4 sem", "Cartographie des risques, FMEA, plans de traitement",
            "Risk Manager", "Risk/FMEA, Ishikawa"),
        new StageDefinition(6, "Cartographie des processus",
            "Établir la cartographie des processus, fiches processus et indicateurs.",
            "4-6 sem", "Cartographie globale, fiches processus, indicateurs",
            "Pilotes de processus", "Workflow Designer (BPMN), Standards Hub"),
        new StageDefinition(7, "Documentation système",
            "Rédiger le manuel, les procédures, modes opératoires et enregistrements.",
            "6-10 sem", "Manuel, procédures, modes opératoires, enregistrements",
            "Tous pilotes", "Document Control"),
        new StageDefinition(8, "Sensibilisation & formation",
            "Former et sensibiliser le personnel aux exigences et au système.",
            "4-6 sem", "Plan de formation, supports, tests",
            "RH + Formation", "Module Formation"),
        new StageDefinition(9, "Mise en œuvre opérationnelle",
            "Appliquer réellement les processus et générer les premiers enregistrements.",
            "8-16 sem", "Application des processus, premiers enregistrements",
            "Toute l'organisation", "Tous modules"),
        new StageDefinition(10, "Audits internes (1er cycle)",
            "Réaliser le programme d'audits internes et consigner les écarts.",
            "4-8 sem", "Programme d'audit, rapports, écarts",
            "Auditeurs internes", "Audit Management"),
        new StageDefinition(11, "Actions correctives",
            "Traiter les écarts via des CAPA et prouver leur efficacité.",
            "4-12 sem", "CAPA, preuves d'efficacité",
            "Pilotes + responsables", "CAPA, Non-conformité"),
        new StageDefinition(12, "Revue de direction",
            "Conduire la revue de direction et décider des ressources/orientations.",
            "1-2 sem", "Compte-rendu de revue, décisions, ressources allouées",
            "Direction", "PDCA, Document Control"),
        new StageDefinition(13, "Pré-audit (optionnel mais recommandé)",
            "Simuler l'audit de certification pour détecter les écarts résiduels.",
            "1 sem", "Rapport d'audit blanc, écarts résiduels",
            "Auditeur tiers ou IA", "Standards Hub (audit blanc IA)"),
        new StageDefinition(14, "Audit de certification — Étape 1 (revue documentaire)",
            "Revue documentaire par l'organisme certificateur.",
            "1-2 j", "Rapport étape 1, points d'attention",
            "Organisme certificateur", "Standards Hub, Document Control"),
        new StageDefinition(15, "Audit de certification — Étape 2 (audit terrain)",
            "Audit terrain par l'organisme certificateur (NC majeures/mineures).",
            "2-5 j", "Rapport étape 2, NC majeures/mineures, certificat",
            "Organisme certificateur", "Standards Hub, tous modules"),
        new StageDefinition(16, "Traitement des NC d'audit",
            "Lever les non-conformités relevées lors de l'audit de certification.",
            "1-3 mois", "Plan d'action, preuves de levée",
            "Pilote", "CAPA, Audit"),
        new StageDefinition(17, "Obtention du certificat",
            "Réception du certificat (valide en général 3 ans).",
            "—", "Certificat valide 3 ans",
            "—", "Standards Hub (registre certificats)"),
        new StageDefinition(18, "Audits de surveillance annuels",
            "Audits de surveillance périodiques par l'organisme certificateur.",
            "1-2 j/an", "Rapports, levée des NC",
            "Organisme certificateur", "Standards Hub"),
        new StageDefinition(19, "Recertification (an 3)",
            "Audit complet de renouvellement du certificat.",
            "3-5 j", "Audit complet de renouvellement",
            "Organisme certificateur", "Standards Hub")
    );
}
