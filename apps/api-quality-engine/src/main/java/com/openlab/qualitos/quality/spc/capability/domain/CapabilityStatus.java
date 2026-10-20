package com.openlab.qualitos.quality.spc.capability.domain;

/**
 * Où se range une étude au regard des seuils de son client.
 *
 * <p>Seul {@link #CAPABLE} vaut « procédé maîtrisé ». {@link #WATCH} reste un
 * procédé non maîtrisé : il est simplement assez proche du seuil pour qu'on le
 * surveille plutôt que de le traiter en urgence — c'est la pastille jaune du
 * registre.
 */
public enum CapabilityStatus {
    /** Cpk au moins égal au seuil de maîtrise (1,67 par défaut). */
    CAPABLE,
    /** Cpk entre le seuil de surveillance (1,33 par défaut) et le seuil de maîtrise. */
    WATCH,
    /** Cpk sous le seuil de surveillance. */
    NOT_CAPABLE;

    public boolean controlled() {
        return this == CAPABLE;
    }
}
