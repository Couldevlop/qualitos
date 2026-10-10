package com.openlab.qualitos.quality.smi.domain;

/**
 * Ce qu'une case de la matrice des exigences dit d'un chapitre pour une norme.
 *
 * <p>Couvert : toutes les exigences du chapitre ont au moins une preuve.
 * Partiel : certaines seulement. Écart : aucune. Sans objet : la norme ne
 * porte aucune exigence sous ce chapitre (la case reste vide plutôt que de
 * prétendre un écart qui n'existe pas).
 */
public enum CoverageStatus {
    COVERED,
    PARTIAL,
    GAP,
    NOT_APPLICABLE;

    public static CoverageStatus of(int covered, int total) {
        if (total <= 0) {
            return NOT_APPLICABLE;
        }
        if (covered <= 0) {
            return GAP;
        }
        return covered >= total ? COVERED : PARTIAL;
    }
}
