package com.openlab.qualitos.quality.apqp;

import java.util.List;

/**
 * Le référentiel APQP qui sert d'AMORÇAGE, réconcilié avec le classeur de suivi
 * du commanditaire ({@code docs/APQP_Tracker.xlsx}, feuille « APQP Tracker »).
 *
 * <p>À la création d'un projet, son cycle reçoit ces cinq phases et leurs
 * quarante-huit livrables, puis le client les adapte : renommer, retirer,
 * ajouter. Partir d'un schéma vide l'obligerait à ressaisir un contenu normatif
 * que personne n'a envie de retaper.
 *
 * <p>Chaque phase et chaque livrable portent une CLÉ. C'est elle qui permet au
 * texte de suivre la langue de l'interface : la part du cycle qui vient du
 * référentiel est écrite dans le code, donc traduisible
 * ({@link ApqpReferenceTranslations}), tandis que ce que le client réécrit lui
 * appartient et reste tel quel. Le texte stocké est le français, langue source
 * du projet : il sert de repli et de valeur d'amorçage.
 *
 * <p>Le classeur compte 48 lignes pour 47 clés distinctes : « Control plan »
 * figure en phase 3 (plan de pré-lancement) ET en phase 4 (plan de production).
 * Même livrable, deux états — d'où deux clés d'ARTEFACT pour une seule clé de
 * livrable, et un artefact porté explicitement plutôt que déduit de la clé.
 *
 * <p>La colonne « PPAP Req'd » du classeur (douze « Y ») devient {@code ppap}.
 * C'est une valeur d'AMORÇAGE et non une marque figée : le client la pilote
 * livrable par livrable, et c'est elle qui remplit son dossier PPAP (ADR 0072).
 */
final class ApqpReference {

    private ApqpReference() {}

    /**
     * Un livrable de référence, avant qu'il n'appartienne à un projet.
     *
     * @param cle         clé de traduction du libellé ; la ligne suit la langue
     *                    tant qu'elle n'est pas retouchée
     * @param ppap        valeur d'amorçage de la colonne « PPAP Req'd »
     * @param artefactCle clé de traduction de l'artefact attendu (colonne D)
     */
    record LivrableModele(String cle, boolean ppap, String artefactCle) {

        /** Le cas ordinaire : l'artefact se déduit de la clé du livrable. */
        static LivrableModele de(String cle, boolean ppap) {
            return new LivrableModele(cle, ppap, cle + ".artifact");
        }

        /** Pour « Control plan », qui paraît deux fois avec deux artefacts. */
        static LivrableModele de(String cle, boolean ppap, String artefactCle) {
            return new LivrableModele(cle, ppap, artefactCle);
        }
    }

    /** Une phase de référence, avant qu'elle n'appartienne à un projet. */
    record PhaseModele(String cle, List<LivrableModele> livrables) {}

    static final List<PhaseModele> PHASES = List.of(
            new PhaseModele("phase.planning", List.of(
                    LivrableModele.de("deliv.product-design-requirements", false),
                    LivrableModele.de("deliv.project-targets", false),
                    LivrableModele.de("deliv.ci-kc-listing", false),
                    LivrableModele.de("deliv.preliminary-bom", false),
                    LivrableModele.de("deliv.preliminary-process-flow", false),
                    LivrableModele.de("deliv.sow-review", false),
                    LivrableModele.de("deliv.preliminary-sourcing-plan", false),
                    LivrableModele.de("deliv.project-plan", false))),

            new PhaseModele("phase.product-design", List.of(
                    LivrableModele.de("deliv.design-risk-analysis", true),
                    LivrableModele.de("deliv.design-records-bom", true),
                    LivrableModele.de("deliv.special-requirements-kc-ci", false),
                    LivrableModele.de("deliv.sourcing-risk-analysis", false),
                    LivrableModele.de("deliv.packaging-specification", false),
                    LivrableModele.de("deliv.design-review-report", false),
                    LivrableModele.de("deliv.build-plan", false),
                    LivrableModele.de("deliv.verification-validation-plans", false),
                    LivrableModele.de("deliv.feasibility-assessment", false))),

            new PhaseModele("phase.process-design", List.of(
                    LivrableModele.de("deliv.process-flow-diagram", true),
                    LivrableModele.de("deliv.floor-plan-layout", false),
                    LivrableModele.de("deliv.production-preparation-plan", false),
                    LivrableModele.de("deliv.staffing-training-plan", false),
                    LivrableModele.de("deliv.pfmea", true),
                    LivrableModele.de("deliv.process-kcs", false),
                    LivrableModele.de("deliv.control-plan", true,
                            "deliv.control-plan.artifact.prelaunch"),
                    LivrableModele.de("deliv.preliminary-capacity", false),
                    LivrableModele.de("deliv.work-station-documentation", false),
                    LivrableModele.de("deliv.msa-plan", false),
                    LivrableModele.de("deliv.supply-chain-risk-plan", false),
                    LivrableModele.de("deliv.handling-packaging-labelling", true),
                    LivrableModele.de("deliv.prr-results", false))),

            new PhaseModele("phase.validation", List.of(
                    LivrableModele.de("deliv.production-run", false),
                    LivrableModele.de("deliv.msa", true),
                    LivrableModele.de("deliv.initial-capability", true),
                    LivrableModele.de("deliv.control-plan", true,
                            "deliv.control-plan.artifact.production"),
                    LivrableModele.de("deliv.capacity-verification", false),
                    LivrableModele.de("deliv.product-validation-results", false),
                    LivrableModele.de("deliv.fair", true),
                    LivrableModele.de("deliv.ppap-file", true),
                    LivrableModele.de("deliv.customer-specific-requirements", true))),

            new PhaseModele("phase.serial-production", List.of(
                    LivrableModele.de("deliv.quality-indices", false),
                    LivrableModele.de("deliv.kpis", false),
                    LivrableModele.de("deliv.targets-met-evidence", false),
                    LivrableModele.de("deliv.otd-capacity-kpis", false),
                    LivrableModele.de("deliv.otd-improvement-plan", false),
                    LivrableModele.de("deliv.closure-recommendations", false),
                    LivrableModele.de("deliv.continuous-improvement", false),
                    LivrableModele.de("deliv.lessons-learned", false),
                    LivrableModele.de("deliv.updated-risk-analyses", false))));
}
