package com.openlab.qualitos.quality.apqp;

/**
 * Ce qu'un projet APQP ouvre.
 *
 * <p>Quatre natures, et non un texte libre : le type décide de ce qu'on attend
 * du projet — une introduction de produit ne se pilote pas comme un transfert
 * d'outillage — et c'est sur lui qu'un directeur qualité filtre sa liste. Un
 * champ libre aurait produit autant d'orthographes que de saisies, donc aucun
 * filtre exploitable.
 *
 * <p>Le jeu reste fermé et {@code OTHER} sert d'issue : plutôt qu'un cinquième
 * type inventé au fil de l'eau, on nomme ce qui ne rentre pas, et l'intitulé du
 * projet dit le reste.
 */
public enum ApqpProjectType {

    /** New Product Introduction : un produit nouveau entre en production. */
    NPI,

    /** Transfer of Work : un produit existant change de site ou de ligne. */
    TOW,

    /** Un produit connu, pour un client qui ne l'était pas. */
    NEW_CUSTOMER,

    /** Tout le reste — requalification, relance, évolution majeure. */
    OTHER
}
