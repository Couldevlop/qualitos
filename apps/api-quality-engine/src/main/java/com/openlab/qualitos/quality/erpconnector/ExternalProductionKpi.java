package com.openlab.qualitos.quality.erpconnector;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO neutre représentant un indicateur de production lu depuis un ERP externe.
 *
 * <p>Le {@code kpiCode} est mappé vers une {@code KpiDefinition} EXISTANTE du tenant
 * (par code). Si aucune définition ne correspond, la mesure est ignorée en WARN —
 * on NE crée JAMAIS de KPI sauvage (CLAUDE.md règle 18.2 #12 : tout KPI a une définition
 * formelle dans le catalogue avant d'être alimenté).
 *
 * @param kpiCode     code de la KpiDefinition cible (ex. "OEE", "SCRAP_RATE")
 * @param value       valeur mesurée
 * @param unit        unité (optionnelle ; à défaut on hérite de la définition)
 * @param periodStart début de période (UNIQUE par (kpi, periodStart))
 * @param periodEnd   fin de période
 */
public record ExternalProductionKpi(
        String kpiCode,
        BigDecimal value,
        String unit,
        Instant periodStart,
        Instant periodEnd
) {}
