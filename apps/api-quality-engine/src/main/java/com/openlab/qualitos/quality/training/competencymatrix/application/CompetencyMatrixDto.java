package com.openlab.qualitos.quality.training.competencymatrix.application;

import java.util.List;
import java.util.UUID;

/** Les commandes et les vues de la matrice de compétences. */
public final class CompetencyMatrixDto {

    private CompetencyMatrixDto() {}

    // ---------- commandes ----------

    public record AddOperatorCommand(UUID userId, String userName, String workshop) {
    }

    public record AddSkillCommand(String name, String category, UUID trainerUserId, String trainerName) {
    }

    public record SetLevelCommand(UUID userId, UUID skillId, int level) {
    }

    // ---------- vues ----------

    public record OperatorView(UUID id, UUID userId, String name, String workshop) {
    }

    /**
     * @param autonomous combien de personnes de la matrice y sont autonomes (niveau 2 ou plus)
     * @param atRisk     moins de deux personnes autonomes
     */
    public record SkillView(UUID skillId, String name, String category, UUID trainerUserId, String trainerName,
                            int autonomous, boolean atRisk) {
    }

    public record CellView(UUID userId, UUID skillId, int level) {
    }

    /**
     * La matrice complète. Les cases absentes valent « non formé » : seules les
     * évaluations posées voyagent.
     */
    public record MatrixView(List<String> workshops, boolean defaultWorkshops, List<OperatorView> operators,
                             List<SkillView> skills, List<CellView> cells) {
    }

    public record WorkshopsView(List<String> names, boolean defaults) {
    }
}
