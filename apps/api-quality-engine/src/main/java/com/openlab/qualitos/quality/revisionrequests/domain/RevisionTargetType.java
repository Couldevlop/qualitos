package com.openlab.qualitos.quality.revisionrequests.domain;

/**
 * Ce que la proposition vise.
 *
 * <p>Les variantes {@code _CREATE} n'ont pas de cible : elles proposent une ligne
 * qui n'existe pas encore. Un mode de défaillance survenu sans exister dans
 * l'analyse est l'écart le plus intéressant à montrer à un auditeur.
 */
public enum RevisionTargetType {
    PFMEA_ITEM,
    PFMEA_ITEM_CREATE,
    CONTROL_PLAN_LINE_CREATE;

    /** Vrai si la proposition crée une ligne au lieu d'en modifier une. */
    public boolean isCreation() {
        return this == PFMEA_ITEM_CREATE || this == CONTROL_PLAN_LINE_CREATE;
    }

    /*
     * Il n'y a délibérément pas de CONTROL_PLAN_LINE : aucune source de ce lot ne
     * propose de modifier une ligne de control plan existante, et une constante
     * que rien ne produit ni n'applique serait du code mort déguisé en contrat.
     * L'ADR 0059 consigne le point ; la rouvrir demandera de décider comment une
     * cible survit à la recopie des lignes dans une nouvelle révision.
     */

    /** Vrai si la proposition porte sur le PFMEA plutôt que sur le control plan. */
    public boolean isPfmea() {
        return this == PFMEA_ITEM || this == PFMEA_ITEM_CREATE;
    }
}
