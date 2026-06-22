package com.openlab.qualitos.quality.standards.normdoc.dossier.infrastructure;

import com.openlab.qualitos.quality.standards.normdoc.domain.NormDocKind;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierDocument.SectionPlan;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierPlan;
import com.openlab.qualitos.quality.standards.normdoc.dossier.domain.DossierPlanProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Plan documentaire par défaut, dérivé de la trame des systèmes de management
 * (HLS — High Level Structure, §8.3) : Manuel Qualité multi-sections, Politique
 * Qualité, et les procédures documentées clés (maîtrise des informations
 * documentées, audit interne, actions correctives, revue de direction).
 *
 * <p>Industry-agnostic (CLAUDE.md §18.2 #9) : aucune exigence sectorielle codée
 * en dur ; les clauses référencées suivent la numérotation HLS commune aux
 * normes ISO de management. L'adaptation fine reste le rôle des Industry Packs et
 * de la rédaction IA (qui borne ses sorties à la norme effectivement résolue).
 */
@Component
public class StaticDossierPlanProvider implements DossierPlanProvider {

    /** Catalogue ordonné des pièces générables, indexé par clé stable. */
    private static final Map<String, DossierDocument> CATALOG = buildCatalog();

    @Override
    public DossierPlan planFor(List<String> requestedDocumentKeys) {
        if (requestedDocumentKeys == null || requestedDocumentKeys.isEmpty()) {
            return new DossierPlan(new ArrayList<>(CATALOG.values()));
        }
        List<DossierDocument> selected = new ArrayList<>();
        for (String key : requestedDocumentKeys) {
            DossierDocument doc = CATALOG.get(key);
            if (doc == null) {
                throw new IllegalArgumentException("Unknown dossier document key: " + key);
            }
            selected.add(doc);
        }
        return new DossierPlan(selected);
    }

    @Override
    public List<DossierDocument> catalog() {
        return new ArrayList<>(CATALOG.values());
    }

    private static Map<String, DossierDocument> buildCatalog() {
        Map<String, DossierDocument> m = new LinkedHashMap<>();

        // 1. Manuel Qualité — multi-sections, structure HLS §4–§10.
        m.put("manuel-qualite", DossierDocument.planned("manuel-qualite", NormDocKind.MANUAL,
                "Manuel Qualité", List.of(
                        new SectionPlan("contexte", "Contexte de l'organisme",
                                List.of("4.1", "4.2", "4.3", "4.4"),
                                "Présenter le périmètre, les enjeux internes/externes et "
                                        + "les parties intéressées."),
                        new SectionPlan("leadership", "Leadership et engagement",
                                List.of("5.1", "5.2", "5.3"),
                                "Décrire l'engagement de la direction, la politique et les rôles."),
                        new SectionPlan("planification", "Planification",
                                List.of("6.1", "6.2", "6.3"),
                                "Risques et opportunités, objectifs, planification des changements."),
                        new SectionPlan("support", "Support",
                                List.of("7.1", "7.2", "7.3", "7.4", "7.5"),
                                "Ressources, compétences, sensibilisation, communication, "
                                        + "informations documentées."),
                        new SectionPlan("realisation", "Réalisation des activités",
                                List.of("8.1"),
                                "Maîtrise opérationnelle des processus."),
                        new SectionPlan("evaluation", "Évaluation des performances",
                                List.of("9.1", "9.2", "9.3"),
                                "Surveillance, audit interne, revue de direction."),
                        new SectionPlan("amelioration", "Amélioration",
                                List.of("10.1", "10.2", "10.3"),
                                "Non-conformités, actions correctives, amélioration continue."))));

        // 2. Politique Qualité.
        m.put("politique-qualite", DossierDocument.planned("politique-qualite", NormDocKind.POLICY,
                "Politique Qualité", List.of(
                        new SectionPlan("engagement", "Engagement de la direction",
                                List.of("5.2"),
                                "Énoncer l'engagement, l'orientation client et l'amélioration "
                                        + "continue."))));

        // 3. Procédure — Maîtrise des informations documentées.
        m.put("proc-maitrise-documents", DossierDocument.planned("proc-maitrise-documents",
                NormDocKind.PROCEDURE, "Procédure — Maîtrise des informations documentées",
                List.of(
                        new SectionPlan("objet", "Objet et domaine d'application",
                                List.of("7.5.1"), "Définir le but et le périmètre."),
                        new SectionPlan("modalites", "Création, mise à jour et maîtrise",
                                List.of("7.5.2", "7.5.3"),
                                "Identification, format, revue, approbation, contrôle des accès et "
                                        + "conservation."))));

        // 4. Procédure — Audit interne.
        m.put("proc-audit-interne", DossierDocument.planned("proc-audit-interne",
                NormDocKind.PROCEDURE, "Procédure — Audit interne", List.of(
                        new SectionPlan("objet", "Objet et responsabilités",
                                List.of("9.2.1"), "Programme d'audit, critères, périmètre."),
                        new SectionPlan("deroulement", "Planification et déroulement",
                                List.of("9.2.2"),
                                "Sélection des auditeurs, conduite, restitution, suivi des écarts."))));

        // 5. Procédure — Actions correctives.
        m.put("proc-actions-correctives", DossierDocument.planned("proc-actions-correctives",
                NormDocKind.PROCEDURE, "Procédure — Non-conformités et actions correctives",
                List.of(
                        new SectionPlan("objet", "Objet",
                                List.of("10.2.1"), "Traitement des non-conformités."),
                        new SectionPlan("traitement", "Analyse et traitement",
                                List.of("10.2.1", "10.2.2"),
                                "Maîtrise, analyse des causes, actions, vérification d'efficacité, "
                                        + "enregistrement."))));

        // 6. Procédure — Revue de direction.
        m.put("proc-revue-direction", DossierDocument.planned("proc-revue-direction",
                NormDocKind.PROCEDURE, "Procédure — Revue de direction", List.of(
                        new SectionPlan("objet", "Objet et fréquence",
                                List.of("9.3.1"), "Cadence et participants."),
                        new SectionPlan("contenu", "Éléments d'entrée et de sortie",
                                List.of("9.3.2", "9.3.3"),
                                "Données d'entrée, décisions, ressources, compte-rendu."))));

        return m;
    }
}
