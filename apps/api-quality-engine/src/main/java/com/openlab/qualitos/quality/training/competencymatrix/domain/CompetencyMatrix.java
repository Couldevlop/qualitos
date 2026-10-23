package com.openlab.qualitos.quality.training.competencymatrix.domain;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * La matrice de compétences : les opérateurs en lignes, les compétences en
 * colonnes, un niveau de 0 à 4 à l'intersection.
 *
 * <p>Objet de domaine pur, sans Spring ni JPA. L'assemblage est une fonction :
 * mêmes entrées, même matrice, dans le même ordre — deux captures d'écran
 * prises à une heure d'écart se comparent case à case.
 *
 * <p><b>Ce que la figure affirme.</b> Une colonne dit qui sait tenir un poste.
 * Quand moins de {@value SkillLevel#MIN_AUTONOMOUS} personnes y sont autonomes,
 * la compétence ne tient qu'à une personne — ou à personne : ce n'est pas une
 * donnée, c'est un risque d'organisation, que la matrice porte en pied de
 * colonne et dans le panneau des risques.
 *
 * @param workshops les ateliers du client, pour l'ordre des lignes
 * @param operators les lignes, rangées par atelier puis par nom
 * @param skills    les colonnes, rangées par catégorie puis par intitulé
 * @param cells     les niveaux connus des opérateurs sur les compétences de la
 *                  matrice ; une case absente vaut « non formé »
 */
public record CompetencyMatrix(Workshops workshops, List<MatrixOperator> operators, List<MatrixSkill> skills,
                               List<LevelCell> cells) {

    /**
     * La couverture d'une compétence : combien de personnes y sont autonomes,
     * et lesquelles (pour nommer « la seule personne autonome »).
     */
    public record Coverage(UUID skillId, int autonomous, List<String> autonomousNames, boolean atRisk) {
    }

    public CompetencyMatrix {
        operators = List.copyOf(operators);
        skills = List.copyOf(skills);
        cells = List.copyOf(cells);
    }

    /**
     * Range les lignes et les colonnes, et écarte les niveaux qui ne tombent sur
     * aucune case : l'évaluation d'une personne qui n'est pas (ou plus) dans la
     * matrice reste au dossier de formation, mais n'a rien à faire ici.
     */
    public static CompetencyMatrix assemble(Workshops workshops, List<MatrixOperator> operators,
                                            List<MatrixSkill> skills, List<LevelCell> levels) {
        Collator collator = Collator.getInstance(Locale.FRENCH);
        collator.setStrength(Collator.SECONDARY);

        List<MatrixOperator> lignes = operators.stream()
                .sorted(Comparator.<MatrixOperator>comparingInt(o -> workshops.rank(o.workshop()))
                        .thenComparing(MatrixOperator::workshop, collator)
                        .thenComparing(MatrixOperator::name, collator)
                        .thenComparing(o -> o.userId().toString()))
                .toList();
        List<MatrixSkill> colonnes = skills.stream()
                .sorted(Comparator.comparing(MatrixSkill::category, Comparator.nullsLast(collator))
                        .thenComparing(MatrixSkill::name, collator)
                        .thenComparing(s -> s.skillId().toString()))
                .toList();

        Set<UUID> personnes = new HashSet<>();
        lignes.forEach(o -> personnes.add(o.userId()));
        Set<UUID> competences = new HashSet<>();
        colonnes.forEach(s -> competences.add(s.skillId()));
        List<LevelCell> cases = levels.stream()
                .filter(c -> personnes.contains(c.userId()) && competences.contains(c.skillId()))
                .toList();
        return new CompetencyMatrix(workshops, lignes, colonnes, cases);
    }

    /** Le niveau d'une personne sur une compétence ; « non formé » s'il n'a jamais été posé. */
    public int level(UUID userId, UUID skillId) {
        return cells.stream()
                .filter(c -> c.userId().equals(userId) && c.skillId().equals(skillId))
                .mapToInt(LevelCell::level)
                .findFirst()
                .orElse(SkillLevel.NOT_TRAINED);
    }

    /** La couverture de chaque compétence, dans l'ordre des colonnes. */
    public List<Coverage> coverage() {
        Map<UUID, String> noms = new HashMap<>();
        operators.forEach(o -> noms.put(o.userId(), o.name()));
        Map<UUID, List<String>> autonomes = new HashMap<>();
        // Parcours dans l'ordre des lignes : « la seule personne autonome » est
        // nommée de façon stable, quel que soit l'ordre rendu par la base.
        Map<UUID, Map<UUID, Integer>> parPersonne = new HashMap<>();
        cells.forEach(c -> parPersonne.computeIfAbsent(c.userId(), k -> new HashMap<>()).put(c.skillId(), c.level()));
        for (MatrixOperator o : operators) {
            Map<UUID, Integer> niveaux = parPersonne.getOrDefault(o.userId(), Map.of());
            niveaux.forEach((skill, level) -> {
                if (SkillLevel.isAutonomous(level)) {
                    autonomes.computeIfAbsent(skill, k -> new ArrayList<>()).add(noms.get(o.userId()));
                }
            });
        }
        return skills.stream()
                .map(s -> {
                    List<String> qui = List.copyOf(autonomes.getOrDefault(s.skillId(), List.of()));
                    return new Coverage(s.skillId(), qui.size(), qui, qui.size() < SkillLevel.MIN_AUTONOMOUS);
                })
                .toList();
    }
}
