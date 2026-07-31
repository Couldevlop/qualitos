package com.openlab.qualitos.quality.dashboards.executive;

import com.openlab.qualitos.quality.kpi.KpiDirection;
import com.openlab.qualitos.quality.kpi.KpiHealth;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contrat du Dashboard exécutif (CLAUDE.md §7.1).
 *
 * <p>Toutes les valeurs proviennent de données réelles du tenant : catalogue KPI et ses
 * mesures, non-conformités, FMEA, CAPA, adoptions normatives. Aucun chiffre n'est
 * fabriqué — l'invariant §18.2 #8 (« aucun KPI affiché sans définition explicite »)
 * est structurellement respecté puisque chaque carte est portée par une définition du
 * catalogue (code, formule, cible, seuils, propriétaire).
 */
public final class ExecutiveDashboardDto {

    private ExecutiveDashboardDto() {}

    /**
     * Carte KPI stratégique. {@code trendDelta} est l'écart absolu entre la dernière
     * mesure et la précédente ({@code null} s'il n'y a pas deux mesures) ; c'est au
     * client d'en faire une flèche, en tenant compte de {@code direction} (une baisse
     * est une bonne nouvelle pour un KPI LOWER_IS_BETTER).
     */
    public record ExecutiveKpiCard(
            UUID kpiId,
            String code,
            String name,
            String description,
            String category,
            String unit,
            KpiDirection direction,
            BigDecimal value,
            BigDecimal targetValue,
            BigDecimal trendDelta,
            KpiHealth health,
            Instant latestPeriodStart,
            Instant latestPeriodEnd
    ) {}

    /** Point de la série de tendance du KPI de tête (12 derniers points max). */
    public record TrendPoint(
            Instant periodStart,
            BigDecimal value,
            BigDecimal targetValue,
            KpiHealth health
    ) {}

    /** Répartition des non-conformités ouvertes par catégorie 6M. */
    public record DefectByCategory(
            String category,
            long count
    ) {}

    /**
     * Risque remonté au comité de direction. Deux sources réelles :
     * item FMEA à RPN élevé ({@code source = "FMEA"}) et CAPA critique en retard
     * ({@code source = "CAPA"}).
     */
    public record TopRisk(
            UUID id,
            String title,
            String source,
            String severity,
            Integer rpn,
            Instant dueDate
    ) {}

    /** Score d'alignement d'une section de norme — alimente la heatmap de conformité. */
    public record SectionScore(
            String sectionCode,
            double score
    ) {}

    /** Score d'alignement d'une norme adoptée (§8.7), avec le détail par section. */
    public record AlignmentBar(
            UUID adoptionId,
            String standardCode,
            String standardName,
            double score,
            String status,
            List<SectionScore> sections
    ) {}

    /**
     * Agrégat complet renvoyé en une requête (le dashboard exécutif est une page :
     * un aller-retour, pas six).
     */
    public record ExecutiveDashboard(
            List<ExecutiveKpiCard> kpis,
            List<TrendPoint> qualityTrend,
            List<DefectByCategory> defectsByCategory,
            List<TopRisk> topRisks,
            List<AlignmentBar> alignment,
            Instant generatedAt
    ) {}
}
