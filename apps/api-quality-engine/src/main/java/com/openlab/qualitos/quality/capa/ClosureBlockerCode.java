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
    OPEN_NON_CONFORMITIES,

    /**
     * Des actions ont promis que leur effet serait vérifié, et ne l'ont pas
     * encore été (décompte = leur nombre).
     *
     * <p>Chaque action porte sa propre exigence de vérification (ADR 0131). Une
     * action « faite » dit seulement que le geste a eu lieu ; elle ne dit pas
     * qu'il a produit son effet. Clore le dossier sans ces vérifications
     * reviendrait à inscrire au registre que la cause est traitée sur la foi
     * d'une promesse que personne n'est allé contrôler — et c'est précisément
     * ce contrôle qu'un auditeur vient chercher (ADR 0132).
     *
     * <p>Une action dont le dernier verdict est PARTIEL ou INEFFICACE compte
     * aussi : elle a été regardée, et ce qu'on a vu ne permet pas de clore.
     */
    ACTION_VERIFICATIONS_PENDING,

    /**
     * Le dossier vient d'un écart sur objectif qualité, et le KPI n'est pas
     * encore revenu sous la cible pour la durée exigée (décompte = le nombre de
     * périodes qu'il RESTE à tenir).
     *
     * <p>Pour ce type de dossier, l'efficacité n'est pas une appréciation : elle
     * EST le retour du KPI sous la cible sur la durée que l'objectif s'est
     * donnée, et l'objectif ne repasse « atteint » qu'à ce moment. Le critère
     * est verrouillé à l'ouverture, pas saisi — sans quoi on pourrait clore sur
     * un critère plus facile que celui qu'on s'était donné (ADR 0133).
     *
     * <p>Le décompte dit ce qu'il RESTE, et non ce qui est acquis : c'est ce que
     * le vérificateur a besoin de savoir pour arrêter de réessayer.
     */
    OBJECTIVE_RECOVERY_PENDING
}
