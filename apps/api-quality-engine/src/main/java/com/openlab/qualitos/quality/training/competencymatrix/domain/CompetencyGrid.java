package com.openlab.qualitos.quality.training.competencymatrix.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La matrice de compétences : compétences en lignes, groupées par famille,
 * collaborateurs en colonnes, un niveau à l'intersection.
 *
 * <p>Objet de domaine, sans Spring ni JPA. L'assemblage est une fonction pure :
 * mêmes entrées, même matrice — condition pour qu'une capture d'écran datée
 * signifie quelque chose.
 *
 * <p><b>Ce que la figure affirme</b>, et qu'une liste de compétences ne dit pas :
 * elle se lit dans les deux sens. Une colonne montre ce qu'une personne couvre.
 * Une LIGNE montre qui sait faire quoi — et c'est celle-là qui intéresse le
 * qualiticien : une seule case remplie sur toute une rangée signale une
 * compétence qui ne tient qu'à une personne. Ce n'est pas une donnée, c'est un
 * risque d'organisation, et la matrice le rend visible sans qu'on le cherche.
 */
public record CompetencyGrid(List<Person> people, List<Group> groups) {

    /** Longueur de l'identifiant abrégé, quand une personne n'a pas de nom. */
    private static final int SHORT_ID = 8;

    /**
     * Niveau à partir duquel on considère qu'une personne DÉTIENT la compétence.
     * Zéro signifie « évalué, et pas encore acquis » : le compter comme détenteur
     * ferait passer pour couverte une compétence que personne ne maîtrise.
     */
    private static final int HOLDER_FROM = 1;

    public record SkillEntry(UUID skillId, String code, String name, String category) {
    }

    public record Assessment(UUID userId, String label, UUID skillId, int level) {
    }

    public record Person(UUID userId, String label) {
    }

    /**
     * @param levels                niveaux alignés sur l'ordre de {@link #people()} ;
     *                              {@code null} là où la personne n'a jamais été
     *                              évaluée sur cette compétence
     * @param holders               nombre de personnes au niveau {@value #HOLDER_FROM}
     *                              ou au-dessus
     * @param singlePointOfKnowledge vrai quand une seule personne détient la
     *                              compétence
     */
    public record Row(UUID skillId, String code, String name, List<Integer> levels,
                      int holders, boolean singlePointOfKnowledge) {
    }

    /** @param category famille de compétences ; {@code null} pour les compétences non classées */
    public record Group(String category, List<Row> rows) {
    }

    public static CompetencyGrid assemble(List<SkillEntry> skills, List<Assessment> assessments) {
        List<Person> people = peopleOf(assessments);
        Map<UUID, Integer> columnOf = new HashMap<>();
        for (int i = 0; i < people.size(); i++) {
            columnOf.put(people.get(i).userId(), i);
        }

        // Une évaluation qui désigne une compétence absente du catalogue est
        // ignorée : la ligne fantôme qu'elle créerait n'aurait ni nom ni famille,
        // et se lirait comme une compétence oubliée alors que c'est l'inverse.
        Map<UUID, Map<UUID, Integer>> levels = new HashMap<>();
        for (Assessment a : assessments) {
            levels.computeIfAbsent(a.skillId(), key -> new HashMap<>()).put(a.userId(), a.level());
        }

        Map<String, List<Row>> byCategory = new LinkedHashMap<>();
        skills.stream()
                .sorted(Comparator.comparing(SkillEntry::name, Comparator.nullsLast(String::compareTo)))
                .forEach(skill -> byCategory
                        .computeIfAbsent(skill.category(), key -> new ArrayList<>())
                        .add(rowOf(skill, people, columnOf, levels.getOrDefault(skill.skillId(), Map.of()))));

        List<Group> groups = byCategory.entrySet().stream()
                // Les familles nommées d'abord, dans l'ordre alphabétique ; les
                // compétences non classées ferment la marche plutôt que d'ouvrir
                // la matrice sur un groupe sans titre.
                .sorted(Map.Entry.comparingByKey(Comparator.nullsLast(String::compareTo)))
                .map(entry -> new Group(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();

        return new CompetencyGrid(people, groups);
    }

    private static Row rowOf(SkillEntry skill, List<Person> people,
                             Map<UUID, Integer> columnOf, Map<UUID, Integer> assessed) {
        Integer[] cells = new Integer[people.size()];
        int holders = 0;
        for (Map.Entry<UUID, Integer> entry : assessed.entrySet()) {
            Integer column = columnOf.get(entry.getKey());
            if (column == null) continue;
            cells[column] = entry.getValue();
            if (entry.getValue() >= HOLDER_FROM) holders++;
        }
        // `Arrays.stream` et non `List.of` : une case jamais évaluée vaut null,
        // et `List.of` refuse les éléments nuls. Le remplacer par un zéro pour
        // contourner la contrainte aurait transformé « on ne sait pas » en
        // « niveau nul, constaté » — exactement ce que ce modèle refuse.
        return new Row(skill.skillId(), skill.code(), skill.name(),
                Arrays.stream(cells).toList(), holders, holders == 1);
    }

    /**
     * Les colonnes sont les personnes réellement évaluées, triées par libellé :
     * l'ordre ne doit rien devoir à celui dans lequel la base a rendu les
     * évaluations, sans quoi la matrice changerait de forme à chaque
     * rafraîchissement et deux captures d'écran ne se compareraient plus.
     */
    private static List<Person> peopleOf(List<Assessment> assessments) {
        Map<UUID, String> labels = new HashMap<>();
        for (Assessment a : assessments) {
            labels.merge(a.userId(), label(a), (existing, candidate) -> existing);
        }
        return labels.entrySet().stream()
                .map(entry -> new Person(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Person::label).thenComparing(p -> p.userId().toString()))
                .toList();
    }

    /**
     * Sans nom enregistré, l'identifiant abrégé. Une colonne sans en-tête serait
     * illisible, et l'identifiant complet mangerait la largeur de la matrice.
     */
    private static String label(Assessment assessment) {
        String label = assessment.label();
        return label == null || label.isBlank()
                ? assessment.userId().toString().substring(0, SHORT_ID)
                : label;
    }
}
