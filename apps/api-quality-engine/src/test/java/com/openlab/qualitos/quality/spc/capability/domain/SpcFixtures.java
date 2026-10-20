package com.openlab.qualitos.quality.spc.capability.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * La grille de référence : celle du classeur {@code docs/spc/SPC Cp Cpk Study
 * Template.xlsx} (« Shaft AX », masse 500 ± 25 g, n = 5, 20 sous-groupes).
 * Le classeur donne pour elle Cp 1,840 et Cpk 1,769 — c'est le chemin doré.
 */
public final class SpcFixtures {

    private SpcFixtures() {}

    public static final double LSL = 475;
    public static final double USL = 525;

    private static final double[][] GOLDEN = {
            {504, 508, 497, 504, 496, 493, 494, 494, 507, 499, 500, 496, 494, 505, 499, 498, 508, 495, 493, 495},
            {497, 505, 508, 497, 498, 507, 498, 502, 501, 507, 504, 505, 494, 495, 495, 498, 506, 497, 499, 497},
            {497, 492, 496, 494, 502, 496, 498, 502, 497, 498, 496, 493, 502, 496, 496, 499, 506, 492, 506, 500},
            {497, 494, 501, 498, 493, 504, 506, 498, 494, 498, 494, 500, 503, 502, 498, 507, 493, 500, 504, 497},
            {498, 492, 496, 500, 494, 507, 497, 502, 500, 499, 500, 499, 494, 502, 500, 500, 503, 502, 498, 492}};

    /** La grille du classeur, mutable pour qu'un test y creuse des trous. */
    public static List<List<Double>> goldenGrid() {
        List<List<Double>> grille = new ArrayList<>();
        for (double[] ligne : GOLDEN) {
            grille.add(new ArrayList<>(Arrays.stream(ligne).boxed().toList()));
        }
        return grille;
    }

    /** Une grille de {@code n} lignes vides. */
    public static List<List<Double>> emptyGrid(int n) {
        List<List<Double>> grille = new ArrayList<>();
        for (int r = 0; r < n; r++) {
            grille.add(new ArrayList<>(java.util.Collections.nCopies(CapabilityCalculation.SUBGROUPS, (Double) null)));
        }
        return grille;
    }

    public static CapabilityStudyDetails goldenDetails() {
        return details(goldenGrid(), 5);
    }

    public static CapabilityStudyDetails details(List<List<Double>> grille, int n) {
        return new CapabilityStudyDetails("Shaft AX", "Weight", "500 ± 25 gram", "M-04",
                UUID.fromString("6f1c2a4e-1f0b-4c47-9d0e-2b8f3a6c7d11"), "J. Martin",
                LocalDate.of(2026, 9, 14), false, LSL, USL, n, grille);
    }
}
