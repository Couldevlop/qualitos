package com.openlab.qualitos.quality.settings.catalog;

/**
 * Jusqu'où le client peut aller sur un catalogue donné.
 *
 * <p>La distinction n'est pas une précaution : elle est imposée par le code. Une
 * liste dont les valeurs commandent des transitions, des calculs ou une exigence
 * normative ne peut pas accueillir une valeur de plus — aucun chemin de code ne
 * la connaîtrait, et la plateforme accepterait une donnée qu'elle ne sait pas
 * traiter. Une taxonomie que personne ne lit en code, au contraire, appartient
 * entièrement au client.
 */
public enum CatalogMode {

    /**
     * Les codes sont ceux de la plateforme : le client renomme et réordonne, il
     * n'ajoute ni ne retire. C'est le cas des listes adossées à une énumération
     * du code (type d'audit, nature de CAPA, catégorie d'écart…).
     */
    RENAME,

    /**
     * La liste entière appartient au client : il ajoute, retire, renomme,
     * réordonne. C'est le cas des taxonomies libres (zones d'audit 5S, secteurs
     * de parties intéressées), stockées en texte côté serveur.
     */
    OPEN
}
