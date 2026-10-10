package com.openlab.qualitos.quality.smi.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * La matrice 5 × 5 des risques ouverts : combien de risques par couple
 * gravité × probabilité.
 *
 * <p>{@code counts.get(g - 1).get(p - 1)} est le nombre de risques de gravité
 * {@code g} et de probabilité {@code p}. L'écran pose la gravité 5 en haut ; le
 * serveur ne présume rien de l'affichage et range par valeur croissante. Une
 * note hors de 1..5 n'existe pas au registre (CHECK en base) : si elle
 * survenait, elle serait ignorée plutôt que de faire tomber le tableau de bord.
 */
public record RiskGrid(List<List<Integer>> counts, int total) {

    public static final int SIZE = 5;

    public RiskGrid {
        counts = counts.stream().map(List::copyOf).toList();
    }

    /** Une note gravité × probabilité ; une note absente (résiduelle non fixée) ne compte pas. */
    public record Rating(Integer severity, Integer probability) {}

    public static RiskGrid of(List<Rating> ratings) {
        int[][] grille = new int[SIZE][SIZE];
        int total = 0;
        for (Rating r : ratings) {
            if (r == null || !inRange(r.severity()) || !inRange(r.probability())) {
                continue;
            }
            grille[r.severity() - 1][r.probability() - 1]++;
            total++;
        }
        List<List<Integer>> lignes = new ArrayList<>(SIZE);
        for (int[] ligne : grille) {
            List<Integer> l = new ArrayList<>(SIZE);
            for (int n : ligne) {
                l.add(n);
            }
            lignes.add(l);
        }
        return new RiskGrid(lignes, total);
    }

    public int at(int severity, int probability) {
        return counts.get(severity - 1).get(probability - 1);
    }

    private static boolean inRange(Integer note) {
        return note != null && note >= 1 && note <= SIZE;
    }
}
