package com.openlab.qualitos.quality.spc.gagerr.application;

import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrAnova;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrDecision;
import com.openlab.qualitos.quality.spc.gagerr.domain.GageRrOperator;
import com.openlab.qualitos.quality.spc.gagerr.domain.VariationComponent;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class GageRrStudyDto {

    private GageRrStudyDto() {}

    /**
     * Une ligne du registre : l'identification, le plan et le verdict enregistré,
     * sans la grille. %GRR en pourcentage (21,0 pour 21 %), à deux décimales.
     */
    public record StudySummary(
            UUID id, String partReference, String characteristic, String instrument, LocalDate studyDate,
            UUID responsibleUserId, String responsibleName, List<GageRrOperator> operators,
            int operatorCount, int partCount, int trialCount, double sigmaMultiplier,
            double grrPercent, double grrTolerancePercent, int ndc, GageRrDecision decision,
            double anovaGrrPercent, int anovaNdc, Instant updatedAt) {}

    /** L'étude complète : la ligne du registre, les limites, la grille et les deux calculs. */
    public record StudySheet(StudySummary study, double lsl, double usl,
                             List<List<List<Double>>> measurements, CalculationView calculation) {}

    /**
     * Les valeurs des étapes 1 à 4 de l'onglet « Résultats », puis l'ANOVA.
     * Les pourcentages sont des fractions (0,21 pour 21 %), comme au classeur.
     */
    public record CalculationView(
            List<List<Double>> cellMeans, List<List<Double>> cellRanges, List<List<Double>> trialMeans,
            List<Double> operatorMeans, List<Double> operatorRanges, List<Double> partMeans,
            double grandMean, double xDiff, double meanRange, double partRange, double rangeUcl,
            double maxRange, int rangesBeyondLimit, double tolerance,
            double k1, double k2, double k3, double d4, double multiplier,
            VariationComponent ev, VariationComponent av, VariationComponent grr, VariationComponent pv,
            VariationComponent tv, int ndc, boolean ndcSufficient, GageRrDecision decision,
            boolean repeatabilityDominant, AnovaView anova) {}

    public record AnovaView(
            List<GageRrAnova.AnovaRow> fullTable, List<GageRrAnova.AnovaRow> pooledTable,
            boolean interactionPooled, GageRrAnova.Variance variance,
            VariationComponent ev, VariationComponent av, VariationComponent operator,
            VariationComponent interaction, VariationComponent grr, VariationComponent pv, VariationComponent tv,
            int ndc, boolean ndcSufficient, GageRrDecision decision) {}

    public record StudyCommand(
            String partReference, String characteristic, String instrument, Double lsl, Double usl,
            Integer operatorCount, Integer partCount, Integer trialCount, Double sigmaMultiplier,
            LocalDate studyDate, UUID responsibleUserId, String responsibleName,
            List<GageRrOperator> operators, List<List<List<Double>>> measurements) {}
}
