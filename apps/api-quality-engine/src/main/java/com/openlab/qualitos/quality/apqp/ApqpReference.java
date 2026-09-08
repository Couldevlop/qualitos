package com.openlab.qualitos.quality.apqp;

import java.util.List;

/**
 * Le référentiel APQP du manuel AIAG, qui sert d'AMORÇAGE.
 *
 * <p>À sa première ouverture de l'écran, un client reçoit ces cinq phases et
 * leurs livrables, puis les adapte : renommer, retirer, ajouter. Partir d'un
 * schéma vide l'obligerait à ressaisir un contenu normatif que personne n'a
 * envie de retaper.
 *
 * <p>Ces libellés ne passent pas par la traduction. Un livrable normatif traduit
 * librement n'est plus le même livrable — « Control Plan » désigne un document
 * précis, pas un plan de contrôle quelconque. Ils sont donc écrits une fois, en
 * français d'usage industriel, et deviennent la propriété du client dès la
 * première copie : c'est lui qui les traduira s'il le souhaite.
 */
final class ApqpReference {

    private ApqpReference() {}

    /** Une phase de référence, avant qu'elle n'appartienne à un client. */
    record PhaseModele(String titre, String objet, String question, List<String> livrables) {}

    static final List<PhaseModele> PHASES = List.of(
            new PhaseModele(
                    "Planifier et définir",
                    "Traduire la voix du client en objectifs de conception mesurables.",
                    "Que demande le client, et qu'est-ce que cela impose au produit ?",
                    List.of(
                            "Voix du client (attentes, réclamations, retours de garantie)",
                            "Plan d'affaires et stratégie marketing",
                            "Étude comparative produit et processus (benchmark)",
                            "Hypothèses produit et processus",
                            "Études de fiabilité produit",
                            "Objectifs de conception",
                            "Objectifs de fiabilité et de qualité",
                            "Nomenclature préliminaire",
                            "Schéma de flux du processus préliminaire",
                            "Liste préliminaire des caractéristiques spéciales",
                            "Plan d'assurance produit",
                            "Engagement de la direction")),

            new PhaseModele(
                    "Conception du produit",
                    "Figer une conception fabricable, vérifiée et documentée.",
                    "Le produit tel que dessiné tient-il ses objectifs, et sait-on le fabriquer ?",
                    List.of(
                            "AMDEC produit (DFMEA)",
                            "Conception pour la fabrication et l'assemblage",
                            "Vérification de la conception",
                            "Revues de conception",
                            "Plan de surveillance prototype",
                            "Dessins et spécifications d'ingénierie",
                            "Spécifications matières",
                            "Modifications de dessins et de spécifications",
                            "Exigences en équipements, outillages et moyens de contrôle",
                            "Caractéristiques spéciales produit et processus",
                            "Engagement de faisabilité de l'équipe")),

            new PhaseModele(
                    "Conception du processus",
                    "Définir le processus de fabrication et ce qui le surveillera.",
                    "Comment fabrique-t-on, et comment saura-t-on que c'est conforme ?",
                    List.of(
                            "Normes d'emballage",
                            "Revue du système qualité produit et processus",
                            "Schéma de flux du processus",
                            "Plan d'implantation des postes",
                            "Matrice des caractéristiques",
                            "AMDEC processus (PFMEA)",
                            "Plan de surveillance de pré-lancement",
                            "Instructions de travail",
                            "Plan d'analyse des systèmes de mesure",
                            "Plan des études de capabilité préliminaires",
                            "Soutien de la direction")),

            new PhaseModele(
                    "Validation",
                    "Prouver sur une production réelle que le processus tient ses capabilités.",
                    "Le processus réel, aux cadences réelles, produit-il conforme ?",
                    List.of(
                            "Essai de production significative",
                            "Analyse des systèmes de mesure (MSA)",
                            "Étude de capabilité préliminaire du processus",
                            "Approbation des pièces de production (PPAP)",
                            "Essais de validation de production",
                            "Évaluation de l'emballage",
                            "Plan de surveillance de production",
                            "Clôture de la planification qualité")),

            new PhaseModele(
                    "Production série et retour d'expérience",
                    "Produire en série, mesurer ce que le client constate, et réduire la variation restante.",
                    "Ce qui sort de la ligne satisfait-il le client, et que corrige-t-on ?",
                    List.of(
                            "Réduction de la variation",
                            "Satisfaction client",
                            "Performance de livraison et de service",
                            "Leçons apprises et bonnes pratiques")));
}
