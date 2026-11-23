package com.openlab.qualitos.quality.derogation.domain;

/** « Pièces à expédier » ou « Pièces déjà expédiées » (section 2). */
public enum ShipmentStage {

    /** Le colis n'est pas parti : l'accord est demandé avant expédition. */
    TO_SHIP,

    /**
     * Les pièces sont déjà chez le client.
     *
     * <p>La demande devient une régularisation, et c'est une information qui
     * change tout pour qui la lit : on ne peut plus retenir le colis.
     */
    ALREADY_SHIPPED
}
