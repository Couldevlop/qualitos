package com.openlab.qualitos.quality.change.domain;

/**
 * Les huit domaines de l'analyse d'impact d'un changement (ADR 0087).
 *
 * <p>L'ordre est celui de l'écran et de l'extraction Excel : la sécurité des
 * personnes d'abord, la formation en dernier. Le code est ce que stockent la
 * base et les paramètres du client ; il ne change jamais.
 */
public enum ImpactDomain {
    SAFETY,
    HEALTH_ENVIRONMENT,
    QUALITY,
    PRODUCTION,
    MAINTENANCE,
    COSTS,
    REGULATORY,
    TRAINING
}
