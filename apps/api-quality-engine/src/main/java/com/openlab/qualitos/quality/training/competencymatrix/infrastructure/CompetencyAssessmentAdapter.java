package com.openlab.qualitos.quality.training.competencymatrix.infrastructure;

import com.openlab.qualitos.quality.training.UserSkillAssignmentRepository;
import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyAssessmentPort;
import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyGrid;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Les evaluations du tenant.
 *
 * <p>Le libelle de la personne vient de l'evaluation elle-meme : la plateforme
 * n'a pas d'annuaire, et une colonne dont l'en-tete serait un identifiant
 * technique ne se lirait pas. Quand il manque, le domaine abrege l'identifiant
 * plutot que de laisser la colonne anonyme.
 */
@Component
public class CompetencyAssessmentAdapter implements CompetencyAssessmentPort {

    private final UserSkillAssignmentRepository assignments;

    public CompetencyAssessmentAdapter(UserSkillAssignmentRepository assignments) {
        this.assignments = assignments;
    }

    @Override
    public List<CompetencyGrid.Assessment> findAll(UUID tenantId) {
        return assignments.findByTenantId(tenantId).stream()
                .map(a -> new CompetencyGrid.Assessment(
                        a.getUserId(), a.getUserName(), a.getSkillId(), a.getLevel()))
                .toList();
    }
}
