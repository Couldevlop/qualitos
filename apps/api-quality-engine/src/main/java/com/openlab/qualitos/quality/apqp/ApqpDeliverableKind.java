package com.openlab.qualitos.quality.apqp;

/**
 * Le genre d'un livrable : ce que son formulaire demande.
 *
 * <p>Un jeu FERMÉ, et non un formulaire par livrable. Le référentiel en compte
 * une soixantaine ; les coder un par un les figerait et contredirait la règle
 * « rien de sectoriel en dur » — alors qu'ils ne se distinguent que par la
 * nature de ce qu'ils produisent : un document, un enregistrement déjà tenu
 * ailleurs dans QualitOS, des mesures, ou une liste de points à acquitter.
 *
 * <p>Ajouter un genre reste possible et se voit : il faut l'écrire ici, dans la
 * contrainte de la base, et dans le formulaire qui le rend. C'est précisément ce
 * qu'on veut — qu'un cinquième genre soit une décision, pas un effet de bord.
 */
public enum ApqpDeliverableKind {

    /** Le livrable EST un document : spécification, rapport, plan, formulaire. */
    ATTACHMENT,

    /** Le livrable est déjà tenu dans un module QualitOS : on y renvoie. */
    MODULE_LINK,

    /** Le livrable est un jeu de mesures : Cp, Cpk, PPM, OTD. */
    DATA_ENTRY,

    /** Le livrable n'est acquis que si plusieurs points le sont. */
    CHECKLIST
}
