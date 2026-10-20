package com.openlab.qualitos.quality.spc.capability.application;

import com.openlab.qualitos.quality.spc.capability.domain.CapabilityStatus;

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

    /** Les valeurs du tableau des formules ; un indice nul n'a pas pu se calculer. */
    public record CalculationView(
            List<Double> subgroupMeans, List<Double> subgroupRanges, int completeSubgroups,
            Double sumOfMeans, Double grandMean, Double meanRange, Double sigma,
            Double cp, Double cpu, Double cpl, Double cpk,
            Double meanUcl, Double meanLcl, Double rangeUcl, Double rangeLcl,
            double d2, double a2, double d3, double d4,
            Double dispersionPercent, Double centerOffset, Double centeringPercent) {}

    public record StudyCommand(
            String partReference, String characteristic, String specification, String machine,
            UUID leaderUserId, String leaderName, LocalDate studyDate, boolean actionPlan,
            Double lsl, Double usl, Integer subgroupSize, List<List<Double>> measurements) {}

    /** Les seuils de Cpk d'un client ; {@code defaults} : rien n'a encore été réglé. */
    public record SettingsView(BigDecimal capableThreshold, BigDecimal watchThreshold, boolean defaults) {}

    public record SettingsCommand(BigDecimal capableThreshold, BigDecimal watchThreshold) {}
}
