package com.openlab.qualitos.quality.riskregister.domain;

/**
 * La nature d'une action CAPA ouverte depuis un risque.
 *
 * <p>Préventive quand l'écart n'est pas survenu — le cas ordinaire d'un risque ;
 * corrective quand il s'est déjà produit au moins une fois et qu'on en supprime
 * la cause. L'endiguement n'a pas sa place ici : il répond à un effet en cours,
 * pas à un risque.
 */
public enum RiskCapaKind {
    CORRECTIVE,
    PREVENTIVE
}
