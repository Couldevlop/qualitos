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

    /**
     * Un produit déjà en série dont la définition, l'outillage ou un procédé
     * change assez pour rouvrir un cycle APQP resserré.
     *
     * <p>Remplace {@code NEW_CUSTOMER}, qui nommait un contexte commercial et
     * non un travail : « produit connu, client nouveau » ouvre le cycle d'un NPI
     * ou d'un ToW selon ce qui change réellement, et n'apprenait donc rien à qui
     * filtrait la liste. La modification majeure, elle, est le cas le plus
     * fréquent après le NPI, et tombait faute de mieux dans {@code OTHER}.
     */
    MAJOR_MODIFICATION,

    /** Tout le reste — requalification, relance. */
    OTHER
}
