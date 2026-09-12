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
 * <p>Ces libellés ne passent pas par la traduction. Un livrable normatif traduit
 * librement n'est plus le même livrable — « Control plan » désigne un document
 * précis, pas un plan de contrôle quelconque. Ils sont donc écrits une fois,
 * dans la langue du document de référence, et deviennent la propriété du client
 * dès la première copie : c'est lui qui les traduira s'il le souhaite.
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
     * @param amorce sous-points d'une {@code CHECKLIST}, ou intitulés de mesures
     *               d'un {@code DATA_ENTRY} ; vide pour les autres genres.
     */
    record LivrableModele(String libelle, boolean ppap,
                          ApqpDeliverableKind genre, List<String> amorce) {

        /** Un document : le cas de loin le plus fréquent. */
        static LivrableModele piece(String libelle) {
            return new LivrableModele(libelle, false, ApqpDeliverableKind.ATTACHMENT, List.of());
        }

        /** Un document qui compose le dossier PPAP. */
        static LivrableModele piecePpap(String libelle) {
            return new LivrableModele(libelle, true, ApqpDeliverableKind.ATTACHMENT, List.of());
        }

        /** Un enregistrement déjà tenu ailleurs dans QualitOS. */
        static LivrableModele renvoi(String libelle, boolean ppap) {
            return new LivrableModele(libelle, ppap, ApqpDeliverableKind.MODULE_LINK, List.of());
        }

        /** Des mesures, avec les intitulés que le document énumère. */
        static LivrableModele mesures(String libelle, boolean ppap, String... intitules) {
            return new LivrableModele(
                    libelle, ppap, ApqpDeliverableKind.DATA_ENTRY, List.of(intitules));
        }

        /** Des sous-points, dont l'acquittement fait le livrable. */
        static LivrableModele points(String libelle, boolean ppap, String... points) {
            return new LivrableModele(
                    libelle, ppap, ApqpDeliverableKind.CHECKLIST, List.of(points));
        }
    }

    /** Une phase de référence, avant qu'elle n'appartienne à un client. */
    record PhaseModele(String titre, String objet, String question, List<LivrableModele> livrables) {}

    static final List<PhaseModele> PHASES = List.of(
            new PhaseModele(
                    "Planning",
                    "Traduire la voix du client en objectifs de conception mesurables.",
                    "Que demande le client, et qu'est-ce que cela impose au produit ?",
                    List.of(
                            LivrableModele.piece("Product design requirements"),
                            // Le document énumère huit cibles : le livrable n'est
                            // acquis que si les huit le sont, et une pièce jointe
                            // unique les cacherait.
                            LivrableModele.points(
                                    "Project targets – safety, quality/manufacturability,"
                                    + " service life, reliability, durability, maintainability,"
                                    + " schedule, and cost",
                                    false,
                                    "safety", "quality/manufacturability", "service life",
                                    "reliability", "durability", "maintainability",
                                    "schedule", "cost"),
                            LivrableModele.piece(
                                    "Preliminary listing of Critical Items (CIs)"
                                    + " and Key Characteristics (KCs)"),
                            LivrableModele.piece("Preliminary BOM"),
                            LivrableModele.piece("Preliminary process flow diagram"),
                            LivrableModele.piece("SOW review"),
                            LivrableModele.piece("Preliminary sourcing plan"),
                            LivrableModele.piece("Project plan"))),

            new PhaseModele(
                    "Product Design & Development",
                    "Figer une conception fabricable, vérifiée et documentée.",
                    "Le produit tel que dessiné tient-il ses objectifs, et sait-on le fabriquer ?",
                    List.of(
                            // L'analyse de risque de conception EST une AMDEC : on
                            // renvoie au module qui la tient, plutôt que d'en
                            // demander une copie qui vieillirait à part.
                            LivrableModele.renvoi("Design risk analysis", true),
                            LivrableModele.piecePpap(
                                    "Design records and BOM addressing the findings"
                                    + " of the design risk analysis"),
                            LivrableModele.piece("Special requirements, product KCs and CIs listings"),
                            LivrableModele.piece("Preliminary risk analysis of sourcing plan"),
                            LivrableModele.piece("Packaging specification"),
                            LivrableModele.piece("Design review report"),
                            LivrableModele.piece("Development product build plan"),
                            LivrableModele.piece(
                                    "Design verification and validation plans,"
                                    + " and associated results"),
                            LivrableModele.piece("Feasibility assessment"))),

            new PhaseModele(
                    "Process Design & Development",
                    "Définir le processus de fabrication et ce qui le surveillera.",
                    "Comment fabrique-t-on, et comment saura-t-on que c'est conforme ?",
                    List.of(
                            LivrableModele.piecePpap("Process flow diagram"),
                            LivrableModele.piece("Floor plan layout"),
                            LivrableModele.piece("Production preparation plan"),
                            LivrableModele.piece(
                                    "Operator staffing and training plan (Human Resources)"),
                            LivrableModele.renvoi("PFMEA", true),
                            LivrableModele.piece("Process KCs"),
                            LivrableModele.renvoi("Control plan", true),
                            LivrableModele.piece("Preliminary capacity assessment"),
                            LivrableModele.piece("Work station documentation"),
                            LivrableModele.piece("Measurement Systems Analysis (MSA) Plan"),
                            LivrableModele.piece("Supply Chain Risk Management Plan"),
                            LivrableModele.points(
                                    "Material handling, packaging, labelling,"
                                    + " and part marking approvals",
                                    true,
                                    "material handling", "packaging", "labelling", "part marking"),
                            LivrableModele.piece("Production Readiness Review (PRR) results"))),

            new PhaseModele(
                    "Product and Process Validation",
                    "Prouver sur une production réelle que le processus tient ses capabilités.",
                    "Le processus réel, aux cadences réelles, produit-il conforme ?",
                    List.of(
                            LivrableModele.piece("Product from production process run(s)"),
                            LivrableModele.piecePpap("MSA"),
                            // Une étude de capabilité se lit par ses indices : les
                            // demander en clair vaut mieux qu'un rapport dont
                            // personne ne ressort le chiffre.
                            LivrableModele.mesures("Initial process capability studies", true,
                                    "Cp", "Cpk", "Pp", "Ppk"),
                            LivrableModele.renvoi("Control plan", true),
                            LivrableModele.piece("Capacity verification"),
                            LivrableModele.piece("Product validation results"),
                            LivrableModele.piecePpap("First Article Inspection Report (FAIR)"),
                            LivrableModele.piecePpap("PPAP file and approval form"),
                            LivrableModele.piecePpap("Customer specific requirements"))),

            new PhaseModele(
                    "Serial Production and feedback",
                    "Produire en série, mesurer ce que le client constate, et réduire la variation restante.",
                    "Ce qui sort de la ligne satisfait-il le client, et que corrige-t-on ?",
                    List.of(
                            LivrableModele.mesures(
                                    "Quality indices [e.g., CpK, Parts Per Million (PPM),"
                                    + " rejection rates]",
                                    false, "CpK", "PPM", "rejection rate"),
                            LivrableModele.piece("Key Performance Indicators (KPIs)"),
                            LivrableModele.piece("Evidence that project targets have been met"),
                            LivrableModele.mesures(
                                    "On-time Delivery (OTD) and capacity KPIs", false,
                                    "OTD", "capacity"),
                            LivrableModele.piece("OTD and capacity improvement plan"),
                            LivrableModele.piece("Project closure recommendations"),
                            // Les actions d'amélioration vivent dans le PDCA ou la
                            // CAPA : on y renvoie au lieu d'en tenir une liste de
                            // plus, qui divergerait.
                            LivrableModele.renvoi("Continuous improvement actions", false),
                            LivrableModele.piece("Lessons learned"),
                            LivrableModele.renvoi(
                                    "Updated design risk analysis, PFMEA, and control plans",
                                    false))));
}
