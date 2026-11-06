package com.openlab.qualitos.core.partner;

/**
 * Ce qu'un cabinet a le droit de faire chez un client.
 *
 * <p>Deux valeurs, pas davantage. Un catalogue de droits fins serait un second
 * systeme d'habilitations a cote de celui du client (§16), avec ses propres
 * trous : le consultant qui intervient reste un utilisateur ordinaire du
 * client, borne par les memes roles et les memes droits metier. L'accord dit
 * seulement s'il peut ecrire, ou seulement regarder.
 */
public enum EngagementScope {

    /** Le cabinet consulte, et n'ecrit rien. */
    READ,

    /** Le cabinet intervient, dans les limites de ses droits metier. */
    INTERVENE
}
