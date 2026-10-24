package com.openlab.qualitos.quality.training.competencymatrix.domain;

/**
 * L'échelle de la matrice de compétences, celle de l'atelier :
 *
 * <pre>
 *   0 Non formé · 1 Débutant · 2 Autonome · 3 Expert · 4 Formateur
 * </pre>
 *
 * <p>Ce sont les mêmes entiers que ceux d'une évaluation de compétence
 * ({@code training_user_skills.level}) : un niveau posé dans la matrice EST
 * l'évaluation de la personne sur cette compétence, il n'existe pas deux vérités.
 *
 * <p>La règle de couverture est celle de la maquette : une compétence tient
 * quand au moins {@value #MIN_AUTONOMOUS} personnes sont autonomes (niveau
 * {@value #AUTONOMOUS} ou plus). En deçà, elle ne tient qu'à une personne — ou à
 * personne — et c'est un risque d'organisation.
 */
public final class SkillLevel {

    public static final int NOT_TRAINED = 0;
    public static final int AUTONOMOUS = 2;
    public static final int TRAINER = 4;
    public static final int MIN_AUTONOMOUS = 2;

    private SkillLevel() {}

    /** Le niveau, s'il est dans l'échelle ; sinon une erreur qui nomme le champ. */
    public static int require(int level) {
        if (level < NOT_TRAINED || level > TRAINER) {
            throw new CompetencyMatrixValidationException("level",
                    "Level must be between " + NOT_TRAINED + " and " + TRAINER);
        }
        return level;
    }

    public static boolean isAutonomous(int level) {
        return level >= AUTONOMOUS;
    }
}
