package com.openlab.qualitos.quality.kpi;

import java.math.BigDecimal;

/**
 * Calcule le statut santé d'une valeur par rapport aux seuils d'une définition.
 * Isolé pour être testable indépendamment de la couche JPA.
 *
 * Sémantique :
 *   HIGHER_IS_BETTER : on veut une valeur grande.
 *     - value ≥ target                 → OK
 *     - value ≥ thresholdWarning        → WARNING
 *     - value < thresholdCritical       → CRITICAL
 *     - sinon                           → WARNING
 *   LOWER_IS_BETTER : on veut une valeur petite (miroir exact).
 *
 *   Si target/warning/critical sont tous null, le statut est UNKNOWN.
 *   On accepte la définition partielle : seul un sous-ensemble peut être fourni —
 *   on évalue avec ce qu'on a, en privilégiant target puis warning puis critical.
 */
public final class KpiEvaluator {

    private KpiEvaluator() {}

    public static KpiHealth evaluate(KpiDefinition def, BigDecimal value) {
        if (value == null) return KpiHealth.UNKNOWN;
        BigDecimal target = def.getTargetValue();
        BigDecimal warning = def.getThresholdWarning();
        BigDecimal critical = def.getThresholdCritical();
        if (target == null && warning == null && critical == null) return KpiHealth.UNKNOWN;

        return switch (def.getDirection()) {
            case HIGHER_IS_BETTER -> evaluateHigher(value, target, warning, critical);
            case LOWER_IS_BETTER -> evaluateLower(value, target, warning, critical);
        };
    }

    private static KpiHealth evaluateHigher(BigDecimal value, BigDecimal target,
                                            BigDecimal warning, BigDecimal critical) {
        if (target != null && value.compareTo(target) >= 0) return KpiHealth.OK;
        if (critical != null && value.compareTo(critical) < 0) return KpiHealth.CRITICAL;
        if (warning != null && value.compareTo(warning) >= 0) return KpiHealth.WARNING;
        if (warning != null) return KpiHealth.CRITICAL;
        if (target != null) return KpiHealth.WARNING;
        return KpiHealth.OK;
    }

    private static KpiHealth evaluateLower(BigDecimal value, BigDecimal target,
                                           BigDecimal warning, BigDecimal critical) {
        if (target != null && value.compareTo(target) <= 0) return KpiHealth.OK;
        if (critical != null && value.compareTo(critical) > 0) return KpiHealth.CRITICAL;
        if (warning != null && value.compareTo(warning) <= 0) return KpiHealth.WARNING;
        if (warning != null) return KpiHealth.CRITICAL;
        if (target != null) return KpiHealth.WARNING;
        return KpiHealth.OK;
    }
}
