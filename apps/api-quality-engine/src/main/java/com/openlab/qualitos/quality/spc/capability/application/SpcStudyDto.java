package com.openlab.qualitos.quality.spc.capability.application;

import com.openlab.qualitos.quality.spc.capability.domain.CapabilityStatus;
import com.openlab.qualitos.quality.spc.capability.domain.DispersionChart;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class SpcStudyDto {

    private SpcStudyDto() {}

    /**
     * Une ligne du registre : l'identification, le verdict, sans la grille.
     *
     * <p>{@code status} est calculé avec les seuils figés de l'étude, rendus à
     * côté pour que l'écran dise « seuil 1,67 » sans relire le paramétrage.
     */
    public record StudySummary(
            UUID id, String partReference, String characteristic, String specification, String machine,
            UUID leaderUserId, String leaderName, LocalDate studyDate, boolean actionPlan,
            double cp, double cpk, CapabilityStatus status,
            BigDecimal capableThreshold, BigDecimal watchThreshold, Instant updatedAt) {}

    /** L'étude complète : la ligne du registre, les limites, la grille et le calcul. */
    public record StudySheet(
            StudySummary study, double lsl, double usl, int subgroupSize,
            List<List<Double>> measurements, CalculationView calculation) {}

    /**
     * Les valeurs du tableau des formules ; un indice nul n'a pas pu se calculer.
     *
     * <p>Les facteurs sont rendus sous un nom NEUTRE
     * ({@code sigmaDivisor}, {@code meanFactor}, {@code dispersionLower},
     * {@code dispersionUpper}) et non plus {@code d2 / a2 / d3 / d4} : depuis
     * qu'une étude peut se conduire sur une carte X̄ – S, ces quatre nombres
     * valent c4, A3, B3 et B4 une fois sur deux, et les anciens noms auraient
     * menti. {@code dispersionChart} dit lequel des deux jeux est en vigueur.
     */
    public record CalculationView(
            List<Double> subgroupMeans, List<Double> subgroupRanges, int completeSubgroups,
            Double sumOfMeans, Double grandMean, Double meanRange, Double sigma,
            Double cp, Double cpu, Double cpl, Double cpk,
            Double meanUcl, Double meanLcl, Double rangeUcl, Double rangeLcl,
            DispersionChart dispersionChart, double sigmaDivisor, double meanFactor,
            double dispersionLower, double dispersionUpper,
            Double dispersionPercent, Double centerOffset, Double centeringPercent) {}

    public record StudyCommand(
            String partReference, String characteristic, String specification, String machine,
            UUID leaderUserId, String leaderName, LocalDate studyDate, boolean actionPlan,
            Double lsl, Double usl, Integer subgroupSize, List<List<Double>> measurements) {}

    /** Les seuils de Cpk d'un client ; {@code defaults} : rien n'a encore été réglé. */
    public record SettingsView(BigDecimal capableThreshold, BigDecimal watchThreshold, boolean defaults) {}

    public record SettingsCommand(BigDecimal capableThreshold, BigDecimal watchThreshold) {}
}
