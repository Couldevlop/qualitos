package com.openlab.qualitos.quality.standards;

/**
 * Type d'élément du tenant servant de preuve pour une exigence.
 * Pointe vers les modules existants : Document, Audit, CAPA, etc.
 */
public enum EvidenceType {
    DOCUMENT,
    AUDIT,
    CAPA,
    PDCA_CYCLE,
    ISHIKAWA,
    FIVES_AUDIT,
    TRAINING_RECORD,
    KPI_RECORD,
    EXTERNAL_FILE,
    OTHER
}
