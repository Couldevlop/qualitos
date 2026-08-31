package com.openlab.qualitos.quality.capa;

/**
 * Raison d'ouverture d'un dossier CAPA (§4.2).
 *
 * <p>À ne pas confondre avec {@link CapaActionType}, qui qualifie chaque action
 * prise à l'intérieur du dossier. Celui-ci dit pourquoi le dossier existe.
 */
public enum CapaType {

    /**
     * Dossier ouvert pour contenir un effet en cours, avant même de savoir d'où
     * il vient : un lot suspect est bloqué, une ligne arrêtée, un client prévenu.
     *
     * <p>Ce n'est pas un dossier correctif au rabais. Un endiguement se juge sur
     * la rapidité et l'étendue de la protection, pas sur la disparition de la
     * cause — et le confondre avec un correctif laisse croire que le problème
     * ne peut plus revenir alors que rien n'a encore été corrigé (ISO 9001
     * §10.2 a), 8D étape D3).
     */
    CONTAINMENT,

    /** Dossier ouvert pour supprimer la cause d'un écart survenu. */
    CORRECTIVE,

    /** Dossier ouvert pour supprimer la cause d'un écart qui n'est pas survenu. */
    PREVENTIVE
}
