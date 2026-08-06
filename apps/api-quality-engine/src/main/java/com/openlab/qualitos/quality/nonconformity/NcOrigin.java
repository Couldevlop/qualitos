package com.openlab.qualitos.quality.nonconformity;

/**
 * D'où vient la non-conformité.
 *
 * <p>Une NC détectée par l'organisation elle-même — autocontrôle, audit interne,
 * revue — et une NC signalée du dehors — client, fournisseur, autorité,
 * organisme certificateur — ne se traitent pas de la même façon : ni les délais,
 * ni l'obligation de réponse, ni les destinataires. Les compter séparément est
 * la première chose que demande une revue de direction.
 */
public enum NcOrigin {

    /** Détectée en interne. Défaut : c'est le cas majoritaire. */
    INTERNAL,

    /** Signalée de l'extérieur : client, fournisseur, autorité, certificateur. */
    EXTERNAL;

    /**
     * Origine par défaut. Classer d'office en « externe » une NC dont personne
     * n'a précisé la provenance gonflerait à tort l'indicateur le plus exposé
     * vis-à-vis des clients.
     */
    public static NcOrigin orDefault(NcOrigin origin) {
        return origin == null ? INTERNAL : origin;
    }
}
