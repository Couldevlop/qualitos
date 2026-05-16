package com.openlab.qualitos.quality.kpi;

/**
 * Sens d'optimisation d'un KPI (§6.2).
 *
 * HIGHER_IS_BETTER : on cherche à monter (OEE, FPY, score satisfaction).
 * LOWER_IS_BETTER  : on cherche à descendre (DPMO, scrap rate, MTTR, COQ).
 */
public enum KpiDirection {
    HIGHER_IS_BETTER,
    LOWER_IS_BETTER
}
