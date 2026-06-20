package com.openlab.qualitos.quality.standards.normdoc.domain;

/**
 * Type de document normatif généré (Standards Hub §8.8). Liste fermée : le
 * générateur IA ne rédige que ces trois familles de documents pré-remplis.
 */
public enum NormDocKind {
    /** Manuel Qualité. */
    MANUAL,
    /** Politique Qualité. */
    POLICY,
    /** Procédure documentée. */
    PROCEDURE
}
