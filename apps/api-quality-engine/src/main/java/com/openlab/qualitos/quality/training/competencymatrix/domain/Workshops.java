package com.openlab.qualitos.quality.training.competencymatrix.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Les ateliers du client : la liste dans laquelle on range un opérateur de la
 * matrice, et par laquelle on la filtre.
 *
 * <p>Paramétrage, donc tenu par l'administrateur du client. Un client qui n'a
 * rien réglé travaille avec les ateliers de la plateforme
 * ({@link #DEFAULT_NAMES}) : la matrice s'utilise dès le premier jour, sans
 * attendre qu'un administrateur passe par les paramètres.
 *
 * @param names    les ateliers, dans l'ordre où l'écran les propose
 * @param defaults vrai quand ce sont ceux de la plateforme, faute de réglage
 */
public record Workshops(List<String> names, boolean defaults) {

    public static final List<String> DEFAULT_NAMES = List.of("Usinage", "Assemblage", "Qualité", "Maintenance");
    public static final int MAX_COUNT = 30;
    public static final int MAX_LENGTH = 60;

    public Workshops {
        names = List.copyOf(names);
    }

    public static Workshops platformDefaults() {
        return new Workshops(DEFAULT_NAMES, true);
    }

    /**
     * La liste réglée par l'administrateur, nettoyée : espaces de bord retirés.
     * Refusée vide, trop longue, avec un nom vide ou trop long, ou avec deux noms
     * qui ne diffèrent que par la casse — deux orthographes deviendraient deux
     * ateliers, et le filtre en perdrait la moitié des opérateurs.
     */
    public static Workshops configured(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new CompetencyMatrixValidationException("names", "At least one workshop is required");
        }
        if (raw.size() > MAX_COUNT) {
            throw new CompetencyMatrixValidationException("names", "At most " + MAX_COUNT + " workshops");
        }
        List<String> names = new ArrayList<>(raw.size());
        Set<String> seen = new HashSet<>();
        for (String r : raw) {
            String name = r == null ? "" : r.strip();
            if (name.isEmpty() || name.length() > MAX_LENGTH) {
                throw new CompetencyMatrixValidationException("names",
                        "A workshop name is 1 to " + MAX_LENGTH + " characters");
            }
            if (!seen.add(key(name))) {
                throw new CompetencyMatrixValidationException("names", "Duplicate workshop: " + name);
            }
            names.add(name);
        }
        return new Workshops(names, false);
    }

    /** Le nom tel que la liste l'écrit, pour une saisie qui ne diffère que par la casse ou les espaces. */
    public Optional<String> canonical(String candidate) {
        if (candidate == null) {
            return Optional.empty();
        }
        String k = key(candidate.strip());
        return names.stream().filter(n -> key(n).equals(k)).findFirst();
    }

    /** Rang d'un atelier dans la liste ; un atelier retiré depuis passe après les autres. */
    public int rank(String name) {
        int i = names.indexOf(name);
        return i < 0 ? names.size() : i;
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
