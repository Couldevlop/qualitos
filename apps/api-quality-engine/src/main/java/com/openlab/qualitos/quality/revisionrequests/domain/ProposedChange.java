package com.openlab.qualitos.quality.revisionrequests.domain;

/**
 * Ce que la proposition changerait, sous une forme qu'un humain peut lire avant
 * de trancher.
 *
 * <p>Pour une modification : le champ visé et ses valeurs avant/après. Pour une
 * création : {@code draftJson}, l'ébauche de la ligne à ajouter. Les deux ne se
 * mélangent pas — une proposition qui dirait à la fois « passe l'occurrence de 4
 * à 6 » et « crée cette ligne » ne serait pas acceptable d'un seul clic.
 *
 * @param field     nom du champ visé, {@code null} pour une création
 * @param from      valeur actuelle, telle qu'affichée à l'utilisateur
 * @param to        valeur proposée
 * @param draftJson ébauche sérialisée de la ligne à créer, {@code null} sinon
 */
public record ProposedChange(String field, String from, String to, String draftJson) {

    /** Le changement d'une cote : le cas courant, écrit une fois pour toutes. */
    public static ProposedChange rating(String field, int from, int to) {
        return new ProposedChange(field, String.valueOf(from), String.valueOf(to), null);
    }

    public static ProposedChange creation(String draftJson) {
        return new ProposedChange(null, null, null, draftJson);
    }
}
