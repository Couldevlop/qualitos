package com.openlab.qualitos.core.partner;

/**
 * Ou en est l'accord d'un client avec un cabinet.
 *
 * <p>Un accord expire n'est PAS un accord revoque, et c'est pour cela qu'il y a
 * quatre valeurs et non un simple booleen : le premier se renouvelle, le second
 * a ete repris, et un rapport d'audit doit savoir lequel des deux s'est produit
 * dix-huit mois plus tard.
 */
public enum EngagementStatus {

    /** L'editeur a constitue le portefeuille. Le client n'a pas repondu : aucun acces. */
    PROPOSED,

    /** Le client a accorde. C'est le seul etat qui ouvre quoi que ce soit. */
    GRANTED,

    /** Le client a refuse d'emblee. */
    DECLINED,

    /** Le client a repris ce qu'il avait accorde. */
    REVOKED
}
