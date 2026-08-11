package com.openlab.qualitos.quality.capa;

/**
 * Ce qui s'oppose à la clôture d'un dossier CAPA, énuméré plutôt que rédigé.
 *
 * <p>Ces codes existent pour que l'écran puisse dire à l'avance ce qui manque.
 * Auparavant, le refus n'arrivait qu'après le clic, sous la forme d'un 409
 * portant une phrase anglaise construite par le service : l'utilisateur
 * découvrait l'obstacle au moment où il croyait en avoir fini, et devait
 * instruire lui-même ce qu'il restait à faire.
 *
 * <p>Un code se traduit, se teste et se compte. Une phrase, non.
 */
public enum ClosureBlockerCode {

    /**
     * Le dossier ne porte aucune action. Rien n'a donc été décidé, et il n'y a
     * rien dont on puisse vérifier l'efficacité.
     */
    NO_ACTION,

    /** Des actions restent à faire (décompte = celles qui ne sont pas DONE). */
    ACTIONS_NOT_DONE,

    /**
     * Toutes les actions du dossier sont des mesures d'endiguement.
     *
     * <p>Elles arrêtent l'effet sans supprimer la cause : clore là-dessus
     * reviendrait à déclarer réglé un problème qui reviendra, et le registre
     * porterait un dossier clos pour une cause jamais traitée. Le décompte est
     * le nombre de mesures d'endiguement, qui est aussi le nombre total
     * d'actions — c'est précisément ce qui fait l'obstacle.
     */
    CONTAINMENT_ONLY,

    /**
     * Des non-conformités rattachées au dossier sont encore ouvertes
     * (décompte = leur nombre).
     */
    OPEN_NON_CONFORMITIES
}
