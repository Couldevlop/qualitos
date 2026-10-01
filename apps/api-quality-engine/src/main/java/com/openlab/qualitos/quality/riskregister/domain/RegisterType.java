package com.openlab.qualitos.quality.riskregister.domain;

/**
 * Le domaine du système de management concerné par un risque ou une opportunité.
 *
 * <p>Un registre unique pour tout le SMI (ISO 9001, 14001, 45001, 27001 partagent
 * la clause 6.1) : c'est ce champ, et non un registre par norme, qui dit de quel
 * système relève la ligne.
 */
public enum RegisterType {
    QUALITY,
    ENVIRONMENT,
    HEALTH_SAFETY,
    INFORMATION_SECURITY,
    LEGAL
}
