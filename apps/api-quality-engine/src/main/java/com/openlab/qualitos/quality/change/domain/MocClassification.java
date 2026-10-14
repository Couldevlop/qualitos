package com.openlab.qualitos.quality.change.domain;

/**
 * La classification d'un changement, déduite de son score d'impact.
 *
 * <p>{@link #NOT_ASSESSED} tant que les huit domaines ne sont pas évalués : un
 * score partiel classerait « mineur » un changement dont on n'a regardé que la
 * moitié, et c'est précisément ce que la gestion des changements doit empêcher.
 */
public enum MocClassification {
    NOT_ASSESSED,
    MINOR,
    MAJOR,
    CRITICAL;

    /** Les classifications qui portent une liste de validateurs. */
    public boolean assessed() {
        return this != NOT_ASSESSED;
    }
}
