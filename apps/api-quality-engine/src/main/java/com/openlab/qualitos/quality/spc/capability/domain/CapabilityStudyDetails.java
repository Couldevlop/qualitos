package com.openlab.qualitos.quality.spc.capability.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Ce que l'utilisateur saisit dans une étude : l'identification et la grille.
 *
 * <p>Le meneur de l'étude est une personne de l'annuaire : son compte
 * ({@code leaderUserId}) et son nom recopié pour l'affichage.
 *
 * @param measurements {@code subgroupSize} lignes (une par mesure du sous-groupe) d'autant de
 *                     cellules que l'étude compte de sous-groupes, une cellule vide valant {@code null}
 */
public record CapabilityStudyDetails(
        String partReference,
        String characteristic,
        String specification,
        String machine,
        UUID leaderUserId,
        String leaderName,
        LocalDate studyDate,
        boolean actionPlan,
        Double lsl,
        Double usl,
        Integer subgroupSize,
        List<List<Double>> measurements) {}
