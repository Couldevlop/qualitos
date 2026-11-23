package com.openlab.qualitos.quality.derogation.domain;

/**
 * L'état d'une demande de dérogation, du brouillon à la réponse du client.
 *
 * <p>Un seul axe. Les quatre statuts annoncés par la présentation — acceptée,
 * refusée, en attente interne, en attente client — sont la <b>suite</b> du cycle
 * dont la maquette porte le début (brouillon, signature demandée, signé), et non
 * un second axe à raccorder.
 *
 * <p><b>Le manager ne refuse pas.</b> S'il n'accepte pas, le dossier passe
 * {@link #TO_AMEND} et revient à l'initiateur, qui corrige et le renvoie —
 * autant de fois qu'il le faut, jusqu'à la signature. Il n'y a donc qu'une
 * issue favorable en interne, {@link #QUALITY_APPROVED}, et la seule sortie par
 * le bas est le retrait par l'initiateur.
 */
public enum DerogationStatus {

    /** Le brouillon, chez l'initiateur. */
    DRAFT,

    /** La signature est demandée au manager désigné : « En attente interne ». */
    PENDING_INTERNAL,

    /** Le manager renvoie pour correction, motif obligatoire : « À modifier ». */
    TO_AMEND,

    /**
     * Le manager a signé : « Approuvé qualité ».
     *
     * <p>Encore <b>interne</b> — rien n'est parti au client à ce stade. Le
     * libellé nomme qui a tranché, ce qu'aucun autre statut ne fait, et ne se
     * confond pas avec l'« Acceptée » du client.
     */
    QUALITY_APPROVED,

    /** Le courriel est parti, on attend la réponse : « En attente client ». */
    PENDING_CUSTOMER,

    /**
     * Le client a dit oui.
     *
     * <p>Y compris « acceptée sous conditions » : la nuance se lit sur
     * {@link CustomerDecision}, qui la garde, mais l'état du dossier est le
     * même — le client a donné son accord.
     */
    ACCEPTED,

    /** Le client a dit non. */
    REFUSED;

    /** Un dossier terminé : plus rien ne l'attend. */
    public boolean isTerminal() {
        return this == ACCEPTED || this == REFUSED;
    }

    /**
     * La fiche est-elle modifiable ?
     *
     * <p>Hors brouillon, la maquette désactive tous les champs. « À modifier »
     * est un brouillon qui porte un motif : c'est précisément l'état où l'on
     * attend une correction.
     */
    public boolean isEditable() {
        return this == DRAFT || this == TO_AMEND;
    }
}
