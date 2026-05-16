package com.openlab.qualitos.quality.dmaic;

/**
 * 5 phases d'un projet Six Sigma DMAIC.
 */
public enum DmaicPhase {
    DEFINE, MEASURE, ANALYZE, IMPROVE, CONTROL;

    /** Phase suivante dans le cycle linéaire. null après CONTROL. */
    public DmaicPhase next() {
        return switch (this) {
            case DEFINE -> MEASURE;
            case MEASURE -> ANALYZE;
            case ANALYZE -> IMPROVE;
            case IMPROVE -> CONTROL;
            case CONTROL -> null;
        };
    }
}
