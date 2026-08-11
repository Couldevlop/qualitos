package com.openlab.qualitos.quality.nonconformity.storage;

/**
 * Module qui revendique des objets binaires dans le stockage.
 *
 * <p>Le balayeur d'orphelins ne connaît aucun domaine — il voit des clés. C'est
 * ici que chaque module qui dépose des binaires déclare ce qui lui appartient
 * encore. Un objet qu'AUCUN propriétaire ne revendique est un orphelin.
 *
 * <p>Ce point d'extension existe pour une raison précise : câbler le balayeur
 * directement sur les deux dépôts d'aujourd'hui (preuves CAPA, photos de NC)
 * marcherait — et le jour où un troisième module déposerait un binaire, le
 * balayeur effacerait ses fichiers en silence, faute de savoir qu'ils existent.
 * Un oubli de ce genre ne se voit qu'après coup, quand la pièce a disparu.
 * Déclarer son appartenance est donc la condition d'entrée : qui ne se déclare
 * pas se fait effacer.
 */
public interface StoredObjectOwner {

    /**
     * Vrai si cette clé correspond à une ligne encore vivante de ce module.
     *
     * <p>En cas de doute — panne du dépôt, erreur inattendue — une
     * implémentation doit laisser remonter l'exception plutôt que répondre
     * {@code false} : le balayeur préfère ne rien supprimer que supprimer à tort.
     */
    boolean isReferenced(String objectKey);

    /** Nom court du module, pour le journal du balayage. */
    String ownerName();
}
