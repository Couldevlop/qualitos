package com.openlab.qualitos.quality.spc.gagerr.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * La grille de référence : celle de l'onglet « Données » du classeur
 * {@code docs/spc/Etude_RR_Gage_1.xlsx} (arbre AX-210, diamètre extérieur,
 * 10,00 ± 0,10 mm, 3 opérateurs × 10 pièces × 3 essais, 5,15 σ).
 *
 * <p>Les valeurs attendues des tests ont été calculées indépendamment
 * (numpy pour la moyenne et l'étendue et l'ANOVA, mpmath pour la loi F), pas
 * relues dans le code testé.
 */
public final class GageRrFixtures {

    private GageRrFixtures() {}

    public static final double LSL = 9.9;
    public static final double USL = 10.1;
    public static final UUID OPERATEUR_A = UUID.fromString("0b6f5c1e-1a2b-4c3d-8e9f-000000000001");
    public static final UUID OPERATEUR_B = UUID.fromString("0b6f5c1e-1a2b-4c3d-8e9f-000000000002");
    public static final UUID OPERATEUR_C = UUID.fromString("0b6f5c1e-1a2b-4c3d-8e9f-000000000003");
    public static final UUID RESPONSABLE = UUID.fromString("6f1c2a4e-1f0b-4c47-9d0e-2b8f3a6c7d11");

    /** [opérateur][pièce][essai], lignes 13 à 22 du classeur. */
    static final double[][][] WORKBOOK = {
            {{9.955, 9.951, 9.94}, {9.956, 9.966, 9.971}, {9.978, 9.984, 9.981}, {9.993, 9.985, 9.999},
                    {9.997, 10.006, 9.995}, {10.018, 10.016, 10.014}, {10.017, 10.032, 10.022},
                    {10.031, 10.039, 10.03}, {10.071, 10.06, 10.057}, {10.087, 10.073, 10.081}},
            {{9.949, 9.954, 9.943}, {9.979, 9.974, 9.956}, {9.983, 9.997, 9.993}, {9.986, 10.002, 10.009},
                    {10.026, 10.013, 10.022}, {10.02, 10.026, 10.021}, {10.029, 10.041, 10.019},
                    {10.043, 10.046, 10.053}, {10.066, 10.071, 10.055}, {10.086, 10.089, 10.102}},
            {{9.943, 9.93, 9.936}, {9.954, 9.959, 9.966}, {9.975, 9.979, 9.988}, {9.99, 9.989, 9.981},
                    {10.009, 10.0, 10.008}, {9.998, 10.01, 10.007}, {10.042, 10.014, 10.039},
                    {10.029, 10.041, 10.031}, {10.058, 10.047, 10.054}, {10.092, 10.094, 10.077}}};

    /** La grille du classeur, mutable pour qu'un test la dégrade. */
    public static List<List<List<Double>>> workbookGrid() {
        return grid(WORKBOOK);
    }

    public static List<List<List<Double>>> grid(double[][][] valeurs) {
        List<List<List<Double>>> blocs = new ArrayList<>();
        for (double[][] bloc : valeurs) {
            List<List<Double>> lignes = new ArrayList<>();
            for (double[] ligne : bloc) {
                List<Double> essais = new ArrayList<>();
                for (double v : ligne) {
                    essais.add(v);
                }
                lignes.add(essais);
            }
            blocs.add(lignes);
        }
        return blocs;
    }

    public static List<GageRrOperator> operators() {
        return new ArrayList<>(List.of(new GageRrOperator(OPERATEUR_A, "Alice Martin"),
                new GageRrOperator(OPERATEUR_B, "Bruno Petit"), new GageRrOperator(OPERATEUR_C, "Chloé Durand")));
    }

    public static GageRrStudyDetails workbookDetails() {
        return details(workbookGrid(), operators(), 5.15);
    }

    public static GageRrStudyDetails details(List<List<List<Double>>> grille, List<GageRrOperator> operateurs,
                                             Double multiplicateur) {
        return new GageRrStudyDetails("Arbre AX-210", "Diamètre extérieur (mm)",
                "Micromètre numérique 0–25 mm, résolution 0,001 mm", LSL, USL, 3, 10, 3, multiplicateur,
                LocalDate.of(2026, 10, 19), RESPONSABLE, "Jeanne Leroy", operateurs, grille);
    }
}
