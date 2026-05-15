package com.openlab.qualitos.quality.risk;

/**
 * Familles de FMEA supportées (CLAUDE.md §4.5).
 *
 * BOW_TIE est modélisé comme une famille FMEA : items = paires
 * menace/conséquence sur l'événement central. La visualisation papillon
 * relève de la couche UI.
 */
public enum FmeaType {
    PROCESS_FMEA,
    DESIGN_FMEA,
    SYSTEM_FMEA,
    SERVICE_FMEA,
    BOW_TIE
}
