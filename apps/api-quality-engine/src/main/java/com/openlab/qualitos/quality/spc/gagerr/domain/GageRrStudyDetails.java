package com.openlab.qualitos.quality.spc.gagerr.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Ce que l'utilisateur saisit dans une étude R&amp;R : l'identification, le
 * plan, les personnes et la grille.
 *
 * @param measurements {@code operatorCount} blocs de {@code partCount} lignes de
 *                     {@code trialCount} mesures : {@code measurements[opérateur][pièce][essai]}
 */
public record GageRrStudyDetails(
        String partReference,
        String characteristic,
        String instrument,
        Double lsl,
        Double usl,
        Integer operatorCount,
        Integer partCount,
        Integer trialCount,
        Double sigmaMultiplier,
        LocalDate studyDate,
        UUID responsibleUserId,
        String responsibleName,
        List<GageRrOperator> operators,
        List<List<List<Double>>> measurements) {}
