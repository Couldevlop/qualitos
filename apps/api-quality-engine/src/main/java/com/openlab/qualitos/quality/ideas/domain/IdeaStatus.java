package com.openlab.qualitos.quality.ideas.domain;

/**
 * Où en est une idée.
 *
 * <p>Miroir sans framework de {@code ProposalStatus}, l'enum du module cercle :
 * le domaine ne doit dépendre ni de JPA ni de Spring, et le module cercle est un
 * paquet plat hérité qu'on ne veut pas voir remonter jusqu'ici.
 */
public enum IdeaStatus {
    PROPOSED, UNDER_REVIEW, APPROVED, REJECTED, IMPLEMENTED, MEASURED;

    /**
     * Le vote est-il encore ouvert ?
     *
     * <p>Il se ferme dès que l'idée est tranchée : le compteur doit dire
     * l'adhésion au moment de la décision, pas celle d'aujourd'hui.
     */
    public boolean voteOpen() {
        return this == PROPOSED || this == UNDER_REVIEW;
    }
}
