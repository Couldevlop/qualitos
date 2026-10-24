package com.openlab.qualitos.quality.dmaic.resolution.domain;

/**
 * Les trois branches de l'analyse 5 pourquoi : pourquoi le défaut est apparu,
 * pourquoi il a échappé jusqu'au client, et pourquoi le système l'a permis.
 */
public enum WhyBranch {
    OCCURRENCE,
    DETECTION,
    SYSTEMIC
}
