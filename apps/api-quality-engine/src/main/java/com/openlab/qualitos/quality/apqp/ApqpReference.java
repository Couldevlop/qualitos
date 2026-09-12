package com.openlab.qualitos.quality.apqp;

import java.util.List;

/**
 * Le référentiel APQP qui sert d'AMORÇAGE, repris du document de référence
 * {@code docs/APQP delivrables.docx}.
 *
 * <p>À sa première ouverture de l'écran, un client reçoit ces cinq phases et
 * leurs livrables, puis les adapte : renommer, retirer, ajouter. Partir d'un
 * schéma vide l'obligerait à ressaisir un contenu normatif que personne n'a
 * envie de retaper.
 *
 * <p>Chaque phase et chaque livrable portent une CLÉ. C'est elle qui permet au
 * texte de suivre la langue de l'interface : la part du cycle qui vient du
 * référentiel est écrite dans le code, donc traduisible
 * ({@link ApqpReferenceTranslations}), tandis que ce que le client réécrit lui
 * appartient et reste tel quel. Le texte stocké ici est le français, langue
 * source du projet : il sert de repli et de valeur d'amorçage.
 *
 * <p>L'astérisque du document (« this deliverable is a PPAP element ») devient
 * {@code ppap}. C'est ce qui permet au dossier PPAP d'être une VUE du cycle et
 * non une seconde liste à tenir d'accord avec lui.
 */
final class ApqpReference {

    private ApqpReference() {}

    /**
     * Un livrable de référence, avant qu'il n'appartienne à un client.
     *
     * @param cle    clé de traduction ; la ligne suit la langue tant qu'elle n'est
     *               pas retouchée
     * @param amorce clés des sous-points d'une {@code CHECKLIST} ou des mesures
     *               d'un {@code DATA_ENTRY} ; vide pour les autres genres
     */
    record LivrableModele(String cle, boolean ppap,
                          ApqpDeliverableKind genre, List<String> amorce) {

        /** Un document : le cas de loin le plus fréquent. */
        static LivrableModele piece(String cle) {
            return new LivrableModele(cle, false, ApqpDeliverableKind.ATTACHMENT, List.of());
        }

        /** Un document qui compose le dossier PPAP. */
        static LivrableModele piecePpap(String cle) {
            return new LivrableModele(cle, true, ApqpDeliverableKind.ATTACHMENT, List.of());
        }

        /** Un enregistrement déjà tenu ailleurs dans QualitOS. */
        static LivrableModele renvoi(String cle, boolean ppap) {
            return new LivrableModele(cle, ppap, ApqpDeliverableKind.MODULE_LINK, List.of());
        }

        /** Des mesures, avec les intitulés que le document énumère. */
        static LivrableModele mesures(String cle, boolean ppap, String... intitules) {
            return new LivrableModele(cle, ppap, ApqpDeliverableKind.DATA_ENTRY, List.of(intitules));
        }

        /** Des sous-points, dont l'acquittement fait le livrable. */
        static LivrableModele points(String cle, boolean ppap, String... points) {
            return new LivrableModele(cle, ppap, ApqpDeliverableKind.CHECKLIST, List.of(points));
        }
    }

    /** Une phase de référence, avant qu'elle n'appartienne à un client. */
    record PhaseModele(String cle, List<LivrableModele> livrables) {}

    static final List<PhaseModele> PHASES = List.of(
            new PhaseModele("phase.planning", List.of(
                    LivrableModele.piece("deliv.product-design-requirements"),
                    // Le document énumère huit cibles : le livrable n'est acquis que
                    // si les huit le sont, et une pièce jointe unique les cacherait.
                    LivrableModele.points("deliv.project-targets", false,
                            "row.safety", "row.quality-manufacturability", "row.service-life",
                            "row.reliability", "row.durability", "row.maintainability",
                            "row.schedule", "row.cost"),
                    LivrableModele.piece("deliv.ci-kc-listing"),
                    LivrableModele.piece("deliv.preliminary-bom"),
                    LivrableModele.piece("deliv.preliminary-process-flow"),
                    LivrableModele.piece("deliv.sow-review"),
                    LivrableModele.piece("deliv.preliminary-sourcing-plan"),
                    LivrableModele.piece("deliv.project-plan"))),

            new PhaseModele("phase.product-design", List.of(
                    // L'analyse de risque de conception EST une AMDEC : on renvoie au
                    // module qui la tient, plutôt que d'en demander une copie qui
                    // vieillirait à part.
                    LivrableModele.renvoi("deliv.design-risk-analysis", true),
                    LivrableModele.piecePpap("deliv.design-records-bom"),
                    LivrableModele.piece("deliv.special-requirements-kc-ci"),
                    LivrableModele.piece("deliv.sourcing-risk-analysis"),
                    LivrableModele.piece("deliv.packaging-specification"),
                    LivrableModele.piece("deliv.design-review-report"),
                    LivrableModele.piece("deliv.build-plan"),
                    LivrableModele.piece("deliv.verification-validation-plans"),
                    LivrableModele.piece("deliv.feasibility-assessment"))),

            new PhaseModele("phase.process-design", List.of(
                    LivrableModele.piecePpap("deliv.process-flow-diagram"),
                    LivrableModele.piece("deliv.floor-plan-layout"),
                    LivrableModele.piece("deliv.production-preparation-plan"),
                    LivrableModele.piece("deliv.staffing-training-plan"),
                    LivrableModele.renvoi("deliv.pfmea", true),
                    LivrableModele.piece("deliv.process-kcs"),
                    LivrableModele.renvoi("deliv.control-plan", true),
                    LivrableModele.piece("deliv.preliminary-capacity"),
                    LivrableModele.piece("deliv.work-station-documentation"),
                    LivrableModele.piece("deliv.msa-plan"),
                    LivrableModele.piece("deliv.supply-chain-risk-plan"),
                    LivrableModele.points("deliv.handling-packaging-labelling", true,
                            "row.material-handling", "row.packaging",
                            "row.labelling", "row.part-marking"),
                    LivrableModele.piece("deliv.prr-results"))),

            new PhaseModele("phase.validation", List.of(
                    LivrableModele.piece("deliv.production-run"),
                    LivrableModele.piecePpap("deliv.msa"),
                    // Une étude de capabilité se lit par ses indices : les demander en
                    // clair vaut mieux qu'un rapport dont personne ne ressort le chiffre.
                    LivrableModele.mesures("deliv.initial-capability", true,
                            "row.cp", "row.cpk", "row.pp", "row.ppk"),
                    LivrableModele.renvoi("deliv.control-plan", true),
                    LivrableModele.piece("deliv.capacity-verification"),
                    LivrableModele.piece("deliv.product-validation-results"),
                    LivrableModele.piecePpap("deliv.fair"),
                    LivrableModele.piecePpap("deliv.ppap-file"),
                    LivrableModele.piecePpap("deliv.customer-specific-requirements"))),

            new PhaseModele("phase.serial-production", List.of(
                    LivrableModele.mesures("deliv.quality-indices", false,
                            "row.cpk", "row.ppm", "row.rejection-rate"),
                    LivrableModele.piece("deliv.kpis"),
                    LivrableModele.piece("deliv.targets-met-evidence"),
                    LivrableModele.mesures("deliv.otd-capacity-kpis", false,
                            "row.otd", "row.capacity"),
                    LivrableModele.piece("deliv.otd-improvement-plan"),
                    LivrableModele.piece("deliv.closure-recommendations"),
                    // Les actions d'amélioration vivent dans le PDCA ou la CAPA : on y
                    // renvoie au lieu d'en tenir une liste de plus, qui divergerait.
                    LivrableModele.renvoi("deliv.continuous-improvement", false),
                    LivrableModele.piece("deliv.lessons-learned"),
                    LivrableModele.renvoi("deliv.updated-risk-analyses", false))));
}
