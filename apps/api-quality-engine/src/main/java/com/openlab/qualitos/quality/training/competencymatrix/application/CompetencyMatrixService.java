package com.openlab.qualitos.quality.training.competencymatrix.application;

import com.openlab.qualitos.quality.training.competencymatrix.domain.CompetencyGrid;

import java.util.UUID;

/**
 * Cas d'usage — la matrice de competences du tenant.
 *
 * <p>Lecture pure : rien n'est stocke, la grille se reassemble a chaque appel.
 * Une matrice figee mentirait des l'evaluation suivante.
 */
public class CompetencyMatrixService {

    private final SkillCataloguePort skills;
    private final CompetencyAssessmentPort assessments;
    private final TenantProvider tenants;

    public CompetencyMatrixService(SkillCataloguePort skills,
                                   CompetencyAssessmentPort assessments,
                                   TenantProvider tenants) {
        this.skills = skills;
        this.assessments = assessments;
        this.tenants = tenants;
    }

    public CompetencyGrid grid() {
        UUID tenantId = tenants.requireTenantId();
        return CompetencyGrid.assemble(skills.findAll(tenantId), assessments.findAll(tenantId));
    }
}
