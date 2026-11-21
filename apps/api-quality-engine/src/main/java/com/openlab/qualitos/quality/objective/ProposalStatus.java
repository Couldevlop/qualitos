package com.openlab.qualitos.quality.objective;

/**
 * Le sort d'une CAPA proposée sur écart d'objectif.
 *
 * <p>Trois états et pas deux : un rejet n'est pas une absence de décision. Sans
 * {@code REJECTED}, un objectif critique sans dossier ne se distinguerait pas
 * d'un objectif critique que personne n'a regardé — et c'est précisément la
 * question qu'un auditeur pose.
 */
public enum ProposalStatus {

    /** Rédigée par la règle, pas encore soumise au pilote ou pas encore tranchée. */
    PENDING,

    /** Le pilote l'a retenue : un dossier CAPA existe, et la proposition le cite. */
    ACCEPTED,

    /** Le pilote ne l'a pas retenue, et a dit pourquoi. */
    REJECTED
}
