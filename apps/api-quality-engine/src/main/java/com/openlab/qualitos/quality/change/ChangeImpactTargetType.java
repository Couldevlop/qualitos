package com.openlab.qualitos.quality.change;

/**
 * Type d'entité QualitOS impactée par un changement. Volontairement string-based
 * pour permettre l'extension sans migration (vs. FK explicites). Le service
 * vérifie le type lors de l'écriture.
 */
public enum ChangeImpactTargetType {
    DOCUMENT,
    TRAINING_PATH,
    SUPPLIER,
    IOT_DEVICE,
    FMEA_PROJECT,
    PDCA_CYCLE,
    STANDARD,
    OTHER
}
