package com.openlab.qualitos.quality.calibration;

/**
 * Type d'étude MSA (Measurement System Analysis, AIAG / IATF 16949).
 * GAGE_R_R : répétabilité + reproductibilité
 * BIAS     : justesse (écart à la valeur de référence)
 * LINEARITY: linéarité sur l'étendue de mesure
 * STABILITY: stabilité dans le temps
 */
public enum MsaType {
    GAGE_R_R,
    BIAS,
    LINEARITY,
    STABILITY
}
