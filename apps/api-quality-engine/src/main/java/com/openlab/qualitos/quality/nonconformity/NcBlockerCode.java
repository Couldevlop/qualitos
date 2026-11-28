package com.openlab.qualitos.quality.nonconformity;

/**
 * Ce qui s'oppose à l'avancement d'un écart, énuméré plutôt que rédigé.
 *
 * <p>Le refus existait déjà, et il était juste : on ne résout pas un écart dont
 * les actions courent encore, on ne le clôt pas avant le verdict d'efficacité
 * de son dossier. Mais il n'arrivait qu'APRÈS le clic, sous la forme d'un 409
 * portant une phrase anglaise construite par le service. L'utilisateur
 * découvrait l'obstacle au moment où il croyait en avoir fini, et devait
 * instruire lui-même ce qu'il restait à faire.
 *
 * <p>Un code se traduit, se teste et se compte. Une phrase, non. C'est le même
 * choix que pour la clôture d'un dossier ({@link
 * com.openlab.qualitos.quality.capa.ClosureBlockerCode}), et pour la même
 * raison.
 */
public enum NcBlockerCode {

    /**
     * Des dossiers CAPA de l'écart portent des actions non exécutées — ils sont
     * {@code OPEN} ou {@code IN_PROGRESS}. Décompte = leur nombre.
     *
     * <p>Empêche de RÉSOUDRE : résoudre, c'est dire « ce qui devait être fait
     * l'a été », et un dossier en cours dit l'inverse sur la même page.
     *
     * <p>SEULES LES ACTIONS NON EXÉCUTÉES COMPTENT. Une action terminée depuis
     * le 8D dont la vérification promise n'a pas conclu ne retient pas la
     * résolution : l'ADR 0132 borne ce verrou à la clôture, où il se dit par
     * {@code ACTION_VERIFICATIONS_PENDING}. Comptée ici, elle faisait demander
     * de TERMINER une action qui l'était, et n'avançait qu'au prix d'un verdict
     * d'efficacité rendu séance tenante — un effet se mesure sur des semaines,
     * et l'exiger avant la résolution retient l'écart à l'étape d'avant.
     */
    CAPA_ACTIONS_TO_CARRY_OUT,

    /**
     * Des dossiers CAPA de l'écart ne sont pas réglés — ni clos, ni écartés.
     * Décompte = leur nombre.
     *
     * <p>Empêche de CLORE : clore, c'est dire « et ça a marché », ce qui attend
     * le verdict d'efficacité du dossier (ADR 0108). Un dossier {@code RESOLVED}
     * compte donc ici, alors qu'il ne s'oppose pas à la résolution.
     */
    CAPA_CASES_UNSETTLED,

    /**
     * Aucun dossier CAPA ne traite l'écart. Décompte = 0, toujours.
     *
     * <p>Empêche de RÉSOUDRE : résoudre, c'est dire « ce qui devait être fait
     * l'a été », et rien n'a été décidé. L'écart passait « résolu » sans qu'une
     * seule action soit inscrite, et le registre déclarait traité un constat
     * dont il ne restait aucune trace de traitement (ISO 9001 §10.2).
     *
     * <p>Le bouton était éteint sans un mot dans ce cas comme dans les autres :
     * c'est le motif qui dit le geste — ajouter une action.
     */
    NO_CAPA_CASE,

    /**
     * L'écart a été déclaré impactant le PFMEA, et aucune révision du PFMEA
     * n'a encore été actée pour lui. Décompte = 1.
     *
     * <p>Empêche de CLORE : clore, c'est dire « la leçon est tirée ». Un défaut
     * survenu pour de bon et une analyse de risque qui l'ignore encore se
     * contredisent sur la même page, et l'IATF 16949 en fait une exigence —
     * toute non-conformité se répercute sur la PFMEA. Le motif ne naît pas
     * d'une règle que l'écran devine : il naît de la RÉPONSE donnée à la
     * question, et disparaît dès que la révision est acceptée ou que la réponse
     * change.
     */
    PFMEA_REVISION_REQUIRED,

    /**
     * Même chose pour le plan de surveillance. Décompte = 1.
     *
     * <p>Deux motifs plutôt qu'un : les deux documents se révisent séparément,
     * par des gestes distincts, et un motif unique laisserait chercher lequel
     * des deux retient la clôture.
     */
    CONTROL_PLAN_REVISION_REQUIRED
}
