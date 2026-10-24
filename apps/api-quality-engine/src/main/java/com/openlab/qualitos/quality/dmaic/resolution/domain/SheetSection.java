package com.openlab.qualitos.quality.dmaic.resolution.domain;

/**
 * Les blocs de questions de la fiche, dans l'ordre du classeur DMAIC : la phase
 * « Analyser » y est coupée en deux, le confinement d'abord, les causes ensuite.
 * On contient le problème chez le client avant d'en chercher la racine.
 */
public enum SheetSection {
    DEFINE,
    MEASURE,
    CONTAIN,
    ANALYZE,
    IMPROVE,
    CONTROL
}
