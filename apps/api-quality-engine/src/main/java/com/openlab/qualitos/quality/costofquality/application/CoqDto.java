package com.openlab.qualitos.quality.costofquality.application;

import com.openlab.qualitos.quality.costofquality.domain.CoqCategory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CoqDto {

    private CoqDto() {}

    /**
     * Le rapport d'une période : un mois ({@code month} renseigné) ou une année.
     *
     * <p>{@code ratio} vaut conformité / non-conformité, et {@code null} quand
     * il n'y a aucune perte : diviser par zéro n'a pas de sens, et afficher 0
     * dirait le contraire de la réalité.
     *
     * <p>{@code months} n'est rempli qu'en vue annuelle : douze points, un par
     * mois, pour l'histogramme.
     */
    public record ReportView(
            int year, Integer month, String currency,
            List<BlockView> blocks,
            BigDecimal conformanceTotal, BigDecimal nonConformanceTotal, BigDecimal total,
            BigDecimal ratio,
            List<MonthView> months) {}

    public record BlockView(CoqCategory category, List<LineView> lines, BigDecimal total) {}

    /**
     * Une ligne affichée.
     *
     * <p>En vue mois, c'est une imputation : {@code entryId} et tout le détail
     * sont renseignés, {@code entryCount} vaut 1. En vue année, c'est le cumul
     * d'un libellé sur les douze mois : {@code entryId} et le détail sont nuls,
     * {@code entryCount} dit combien d'imputations il agrège.
     */
    public record LineView(
            UUID entryId, UUID labelId, String labelCode, String labelName, boolean partControl,
            BigDecimal amount, int entryCount,
            String responsible, LocalDate imputationDate, String comment,
            String partReference, Integer partQuantity, String lot, LocalDate receivedOrMadeOn) {}

    public record MonthView(int month, BigDecimal conformance, BigDecimal nonConformance) {}

    public record LabelView(UUID id, CoqCategory category, String code, String name,
                            boolean partControl, boolean builtIn) {}

    public record EntryCommand(
            UUID labelId, BigDecimal amount, String responsible, LocalDate imputationDate,
            String comment, String partReference, Integer partQuantity, String lot,
            LocalDate receivedOrMadeOn) {}

    public record LabelCommand(CoqCategory category, String name, boolean partControl) {}
}
