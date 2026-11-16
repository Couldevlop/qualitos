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
    CAPA_CASES_UNSETTLED
}
