package com.openlab.qualitos.quality.nonconformity;

/**
 * La méthode d'amélioration par laquelle un écart se traite (§3.6).
 *
 * <p>Un écart constaté ne se répare pas toujours de la même façon. Un écart
 * mineur ou majeur relève d'un <b>cycle PDCA</b> : on planifie, on agit, on
 * vérifie, et l'affaire se referme en quelques semaines. Un écart critique
 * relève d'un <b>projet DMAIC</b> : la cause est statistique, elle demande des
 * mesures, une analyse de capabilité et une phase de maîtrise — un cycle PDCA
 * s'y briserait, parce qu'il n'a pas de place pour la mesure.
 *
 * <p>La gravité propose, l'utilisateur dispose : {@link #fromSeverity} donne la
 * méthode attendue, et l'écran la montre avant d'agir. Elle n'est pas imposée —
 * un écart mineur sur un procédé déjà sous contrôle statistique peut mériter un
 * DMAIC, et la personne qui tient le dossier le sait mieux qu'une table.
 */
public enum NcMethod {

    /** Cycle PDCA : plan, action, vérification, généralisation. */
    PDCA,

    /** Projet DMAIC : définir, mesurer, analyser, améliorer, maîtriser. */
    DMAIC;

    /**
     * La méthode que la gravité appelle.
     *
     * @param severity la gravité de l'écart, jamais {@code null}
     * @return {@link #DMAIC} pour un écart critique, {@link #PDCA} sinon
     */
    public static NcMethod fromSeverity(NcSeverity severity) {
        return severity == NcSeverity.CRITICAL ? DMAIC : PDCA;
    }
}
