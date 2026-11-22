package com.openlab.qualitos.quality.derogation.domain;

/**
 * La réponse du client, portée à la main au retour de son courriel.
 *
 * <p>Rien ne la rapatrie automatiquement : le client répond hors plateforme, en
 * renvoyant le PDF signé de son côté, et l'initiateur saisit la décision.
 */
public enum CustomerDecision {

    /** Aucune réponse encore : c'est l'état d'un dossier qui vient de partir. */
    PENDING,

    ACCEPTED,

    /**
     * Accepté, mais sous conditions.
     *
     * <p>Elle ne se replie <b>pas</b> sur {@link #ACCEPTED} : elle engage
     * l'expédition autrement — les conditions se lisent dans le commentaire du
     * dossier — et la confondre perdrait au registre une information qui compte
     * pour qui prépare le colis.
     */
    ACCEPTED_WITH_CONDITIONS,

    REFUSED;

    /** L'état que cette réponse donne au dossier. */
    public DerogationStatus toStatus() {
        return switch (this) {
            case PENDING -> DerogationStatus.PENDING_CUSTOMER;
            case ACCEPTED, ACCEPTED_WITH_CONDITIONS -> DerogationStatus.ACCEPTED;
            case REFUSED -> DerogationStatus.REFUSED;
        };
    }
}
